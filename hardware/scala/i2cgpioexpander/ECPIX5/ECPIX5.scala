package i2cgpioexpander

import spinal.core._
import spinal.core.sim._
import spinal.lib._

import nafarr.blackboxes.lattice.ecp5._

import zibal.misc.LatticeTools

import elements.sdk.ElementsApp
import elements.board.ECPIX5

case class ECPIX5Top(p: I2cGpioExpander.Parameter, resetDelay: Int) extends Component {
  val io = new Bundle {
    val clock = LatticeCmosIo(ECPIX5.SystemClock.clock).clock(ECPIX5.SystemClock.frequency)
    val reset = LatticeCmosIo(ECPIX5.Pmods.Pmod0.pin2)
    val i2c = new Bundle {
      val scl = LatticeCmosIo(ECPIX5.Pmods.Pmod0.pin0).pullUp
      val sda = LatticeCmosIo(ECPIX5.Pmods.Pmod0.pin1).pullUp
      val interrupt = LatticeCmosIo(ECPIX5.Pmods.Pmod0.pin3)
    }
    val gpio = Vec(
      LatticeCmosIo(ECPIX5.Buttons.sw0),
      LatticeCmosIo(ECPIX5.LEDs.LD5.blue),
      LatticeCmosIo(ECPIX5.LEDs.LD5.red),
      LatticeCmosIo(ECPIX5.LEDs.LD5.green),
      LatticeCmosIo(ECPIX5.LEDs.LD6.red),
      LatticeCmosIo(ECPIX5.LEDs.LD6.green),
      LatticeCmosIo(ECPIX5.LEDs.LD6.blue),
      LatticeCmosIo(ECPIX5.LEDs.LD8.red)
    )
  }
  val address = B"001"
  val clock = Bool
  val reset = Bool

  io.clock <> FakeI(clock)
  io.reset <> FakeI(reset)

  val resetCtrlClockDomain = ClockDomain(
    clock = clock,
    config = ClockDomainConfig(
      resetKind = BOOT
    )
  )

  val resetCtrl = new ClockingArea(resetCtrlClockDomain) {
    val resetUnbuffered = True
    val counter = Reg(UInt(log2Up(resetDelay) bits)).init(0)
    when(counter =/= U(resetDelay - 1)) {
      counter := counter + 1
      resetUnbuffered := False
    }
    when(BufferCC(reset)) {
      counter := 0
    }

    val systemReset = RegNext(resetUnbuffered)
  }

  val clockCtrl = new Area {
    val mainClockDomain = ClockDomain(
      clock = clock,
      reset = resetCtrl.systemReset,
      frequency = FixedFrequency(ECPIX5.SystemClock.frequency),
      config = ClockDomainConfig(resetKind = spinal.core.SYNC, resetActiveLevel = LOW)
    )
  }

  val system = new ClockingArea(clockCtrl.mainClockDomain) {
    val expander = I2cGpioExpander(p)
    expander.io.address := address
  }

  io.i2c.interrupt <> FakeO(system.expander.io.i2c.interrupts(0))
  io.i2c.scl <> FakeIo(system.expander.io.i2c.scl)
  io.i2c.sda <> FakeIo(system.expander.io.i2c.sda)
  for (index <- 0 until io.gpio.length) {
    io.gpio(index) <> FakeIo(system.expander.io.gpio.pins(index))
  }
}

object ECPIX5Generate extends ElementsApp {
  elementsConfig.genFPGASpinalConfig.generateVerilog {
    val top = ECPIX5Top(I2cGpioExpander.Parameter.default, 128)

    val lpf = LatticeTools.Lpf(elementsConfig)
    lpf.generate(top.io)

    top
  }
}
