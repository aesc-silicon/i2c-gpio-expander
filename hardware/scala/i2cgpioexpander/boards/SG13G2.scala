package i2cgpioexpander.boards
import spinal.core._
import spinal.core.sim._
import spinal.lib._

import i2cgpioexpander.I2cGpioExpander

import nafarr.blackboxes.ihp.sg13g2._

import zibal.misc.OpenROADTools

import elements.sdk.ElementsApp

case class SG13G2Top(p: I2cGpioExpander.Parameter, resetDelay: Int) extends Component {
  val io = new Bundle {
    val clock = IhpCmosIo("south", 0)
    val reset = IhpCmosIo("south", 1, "clk_core")
    val i2c = new Bundle {
      val scl = IhpCmosIo("south", 2, "clk_core")
      val sda = IhpCmosIo("south", 3, "clk_core")
      val interrupt = IhpCmosIo("south", 4, "clk_core")
    }
    val gpio = Vec(
      IhpCmosIo("north", 0, "clk_core"),
      IhpCmosIo("north", 1, "clk_core"),
      IhpCmosIo("north", 2, "clk_core"),
      IhpCmosIo("north", 3, "clk_core"),
      IhpCmosIo("north", 4, "clk_core"),
      IhpCmosIo("west", 0, "clk_core"),
      IhpCmosIo("west", 1, "clk_core"),
      IhpCmosIo("west", 2, "clk_core")
    )
    val address = Vec(
      IhpCmosIo("east", 2, "clk_core"),
      IhpCmosIo("east", 3, "clk_core"),
      IhpCmosIo("east", 4, "clk_core")
    )
  }
  val clock = Bool
  val reset = Bool
  val address = Bits(io.address.length bits)

  io.clock <> IOPadIn(clock)
  io.reset <> IOPadIn(reset)
  for (index <- 0 until io.address.length) {
    io.address(index) <> IOPadIn(address(index))
  }

  val clockCtrl = new Area {
    val mainClockDomain = ClockDomain(
      clock = clock,
      reset = reset,
      frequency = FixedFrequency(50 MHz),
      config = ClockDomainConfig(resetKind = spinal.core.SYNC, resetActiveLevel = LOW)
    )
  }
  val system = new ClockingArea(clockCtrl.mainClockDomain) {
    val expander = I2cGpioExpander(p)
    expander.io.address := address
  }

  io.i2c.interrupt <> IOPadOut4mA(system.expander.io.i2c.interrupts(0))
  io.i2c.scl <> IOPadInOut4mA(system.expander.io.i2c.scl)
  io.i2c.sda <> IOPadInOut4mA(system.expander.io.i2c.sda)
  for (index <- 0 until io.gpio.length) {
    io.gpio(index) <> IOPadInOut16mA(system.expander.io.gpio.pins(index))
  }
}
object SG13G2Generate extends ElementsApp {
  elementsConfig.genASICSpinalConfig.generateVerilog {
    val top = SG13G2Top(I2cGpioExpander.Parameter.default.copy(addressWidth=3), 128)

    val config = OpenROADTools.IHP.Config(elementsConfig)
    config.generate(
      OpenROADTools.PDKs.IHP.sg13g2,
      (0.0, 0.0, 1050, 1050),
      (425.28, 427.16, 631.2, 630.24)
    )

    val sdc = OpenROADTools.IHP.Sdc(elementsConfig)
    sdc.addClock(top.io.clock.PAD, 50 MHz, "clk_core")
    sdc.generate(top.io)

    val io = OpenROADTools.IHP.Io(elementsConfig)
    io.addPad("east", 0, "sg13g2_IOPadVdd")
    io.addPad("east", 1, "sg13g2_IOPadVss")
    io.addPad("west", 3, "sg13g2_IOPadVss")
    io.addPad("west", 4, "sg13g2_IOPadVdd")
    io.generate(top.io)

    val pdn = OpenROADTools.IHP.Pdn(elementsConfig)
    pdn.generate()

    top
  }
}
