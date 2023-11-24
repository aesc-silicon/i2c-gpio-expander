package i2cgpioexpander

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.misc.InterruptCtrl

import nafarr.peripherals.com.i2c.{I2c, I2cCtrl, I2cDeviceCtrl}
import nafarr.peripherals.io.gpio.{Gpio, GpioCtrl}

object I2cGpioExpander {
  def apply(p: Parameter) = I2cGpioExpander(p)

  case class Parameter(
      i2c: I2cCtrl.Parameter,
      gpio: GpioCtrl.Parameter,
      addressWidth: Int = 3,
      deviceAddress: Int = 6,
      timeoutCycles: Int = 5000,
      clockDivider: Int = 1
  ) {}
  object Parameter {
    def default = Parameter(
      I2cCtrl.Parameter(permission = null, memory = null, io = I2c.Parameter(1)),
      GpioCtrl.Parameter.full(8)
    )
  }

  object State extends SpinalEnum {
    val IDLE, REG, READ, WRITE, RESPONSE = newElement()
  }

  case class I2cGpioExpander(p: Parameter) extends Component {
    val io = new Bundle {
      val address = in(Bits(p.addressWidth bits))
      val i2c = slave(I2c.Io(p.i2c.io))
      val gpio = Gpio.Io(p.gpio.io)
    }

    val i2cCtrl = I2cDeviceCtrl(p.i2c)
    i2cCtrl.io.i2c <> io.i2c
    val i2cConfig = new Area {
      val latchedAddress = Reg(Bits(p.addressWidth bits))
      val latch = RegInit(False)

      when(!latch) {
        latchedAddress := io.address
        latch := True
      }

      i2cCtrl.io.config.clockDivider := U(p.clockDivider)
      i2cCtrl.io.config.timeout := U(p.timeoutCycles)
      i2cCtrl.io.config.deviceAddr := B(p.deviceAddress, 7 - p.addressWidth bit) ##
        latchedAddress
    }

    val gpioCtrl = GpioCtrl(p.gpio)
    gpioCtrl.io.gpio <> io.gpio
    val gpioConfig = new Area {
      val write = Reg(Bits(p.gpio.io.width bits)).init(0)
      val direction = Reg(Bits(p.gpio.io.width bits)).init(0)

      gpioCtrl.io.config.write := write
      gpioCtrl.io.config.direction := direction
    }

    val irq = new Area {
      val high = new Area {
        val masks = Reg(Bits(p.gpio.io.width bits)).init(0)
        val ctrl = new InterruptCtrl(p.gpio.io.width)
        ctrl.io.clears := 0
        ctrl.io.masks := masks
      }
      val low = new Area {
        val masks = Reg(Bits(p.gpio.io.width bits)).init(0)
        val ctrl = new InterruptCtrl(p.gpio.io.width)
        ctrl.io.clears := 0
        ctrl.io.masks := masks
      }
      val rise = new Area {
        val masks = Reg(Bits(p.gpio.io.width bits)).init(0)
        val ctrl = new InterruptCtrl(p.gpio.io.width)
        ctrl.io.clears := 0
        ctrl.io.masks := masks
      }
      val fall = new Area {
        val masks = Reg(Bits(p.gpio.io.width bits)).init(0)
        val ctrl = new InterruptCtrl(p.gpio.io.width)
        ctrl.io.clears := 0
        ctrl.io.masks := masks
      }

      high.ctrl.io.inputs <> gpioCtrl.io.irqHigh.valid
      low.ctrl.io.inputs <> gpioCtrl.io.irqLow.valid
      rise.ctrl.io.inputs <> gpioCtrl.io.irqRise.valid
      fall.ctrl.io.inputs := gpioCtrl.io.irqFall.valid

      gpioCtrl.io.irqHigh.pending := high.ctrl.io.pendings
      gpioCtrl.io.irqLow.pending := low.ctrl.io.pendings
      gpioCtrl.io.irqRise.pending := rise.ctrl.io.pendings
      gpioCtrl.io.irqFall.pending := fall.ctrl.io.pendings

      i2cCtrl.io.interrupts := (high.ctrl.io.pendings | low.ctrl.io.pendings |
        rise.ctrl.io.pendings | fall.ctrl.io.pendings).orR.asBits
    }

    val link = new Area {
      val regAddr = Reg(UInt(8 bits))
      val state = RegInit(State.IDLE)
      val data = RegInit(B"00000000")
      val error = RegInit(False)

      i2cCtrl.io.cmd.ready := False
      i2cCtrl.io.rsp.valid := False
      i2cCtrl.io.rsp.payload.data := data
      i2cCtrl.io.rsp.payload.error := error

      switch(state) {
        is(State.IDLE) {
          when(i2cCtrl.io.cmd.valid) {
            error := False
            when(i2cCtrl.io.cmd.payload.read) {
              state := State.READ
            } otherwise {
              when (i2cCtrl.io.cmd.payload.reg) {
                state := State.REG
              } otherwise {
                state := State.WRITE
              }
            }
          }
        }
        is(State.REG) {
          regAddr := i2cCtrl.io.cmd.payload.data.asUInt
          i2cCtrl.io.cmd.ready := True
          state := State.RESPONSE
        }
        is(State.READ) {
          switch(regAddr.asBits) {
            is(B"00000000") {
              data := gpioCtrl.io.value
            }
            is(B"00000001") {
              data := gpioConfig.write
            }
            is(B"00000010") {
              data := gpioConfig.direction
            }
            is(B"00000011") {
              data := irq.high.ctrl.io.pendings
            }
            is(B"00000100") {
              data := irq.low.ctrl.io.pendings
            }
            is(B"00000101") {
              data := irq.rise.ctrl.io.pendings
            }
            is(B"00000110") {
              data := irq.fall.ctrl.io.pendings
            }
            is(B"00000111") {
              data := irq.high.ctrl.io.masks
            }
            is(B"00001000") {
              data := irq.low.ctrl.io.masks
            }
            is(B"00001001") {
              data := irq.rise.ctrl.io.masks
            }
            is(B"00001010") {
              data := irq.fall.ctrl.io.masks
            }
            default {
              error := True
            }
          }
          i2cCtrl.io.cmd.ready := True
          regAddr := regAddr + 1
          state := State.RESPONSE
        }
        is(State.WRITE) {
          switch(regAddr.asBits) {
            is(B"00000001") {
              gpioConfig.write := i2cCtrl.io.cmd.payload.data
            }
            is(B"00000010") {
              gpioConfig.direction := i2cCtrl.io.cmd.payload.data
            }
            is(B"00000011") {
              irq.high.ctrl.io.clears := i2cCtrl.io.cmd.payload.data
            }
            is(B"00000100") {
              irq.low.ctrl.io.clears := i2cCtrl.io.cmd.payload.data
            }
            is(B"00000101") {
              irq.rise.ctrl.io.clears := i2cCtrl.io.cmd.payload.data
            }
            is(B"00000110") {
              irq.fall.ctrl.io.clears := i2cCtrl.io.cmd.payload.data
            }
            is(B"00000111") {
              irq.high.masks := i2cCtrl.io.cmd.payload.data
            }
            is(B"00001000") {
              irq.low.masks := i2cCtrl.io.cmd.payload.data
            }
            is(B"00001001") {
              irq.rise.masks := i2cCtrl.io.cmd.payload.data
            }
            is(B"00001010") {
              irq.fall.masks := i2cCtrl.io.cmd.payload.data
            }
            default {
              error := True
            }
          }
          i2cCtrl.io.cmd.ready := True
          regAddr := regAddr + 1
          state := State.RESPONSE
        }
        is(State.RESPONSE) {
          i2cCtrl.io.rsp.valid := True
          when(i2cCtrl.io.rsp.ready) {
            state := State.IDLE
          }
        }
      }
    }
  }
}
