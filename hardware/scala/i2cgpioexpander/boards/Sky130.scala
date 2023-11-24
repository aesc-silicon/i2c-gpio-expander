package i2cgpioexpander.boards
import spinal.core._
import spinal.core.sim._
import spinal.lib._

import i2cgpioexpander.I2cGpioExpander

import nafarr.blackboxes.skywater.sky130._

import zibal.misc.OpenROADTools

import elements.sdk.ElementsApp

case class Sky130Top(p: I2cGpioExpander.Parameter, resetDelay: Int) extends Component {
  val io = new Bundle {
    val clock = Sky130CmosIo("south", 200)
    val reset = Sky130CmosIo("south", 290)
    val i2c = new Bundle {
      val scl = Sky130CmosIo("south", 560)
      val sda = Sky130CmosIo("south", 650)
      val interrupt = Sky130CmosIo("east", 204)
    }
    val gpio = Vec(
      Sky130CmosIo("north", 200),
      Sky130CmosIo("north", 290),
      Sky130CmosIo("north", 560),
      Sky130CmosIo("north", 650),
      Sky130CmosIo("west", 654),
      Sky130CmosIo("west", 564),
      Sky130CmosIo("west", 294),
      Sky130CmosIo("west", 204)
    )
    val address = Vec(
      Sky130CmosIo("east", 294),
      Sky130CmosIo("east", 564),
      Sky130CmosIo("east", 654)
    )
  }
  val clock = Bool
  val reset = Bool
  val address = Bits(3 bits)

  io.clock <> top_gpiov2().asInput(clock)
  io.reset <> top_gpiov2().asInput(reset)
  for (index <- 0 until io.address.length) {
    io.address(index) <> top_gpiov2().asInput(address(index))
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

  io.i2c.interrupt <> top_gpiov2().asOutput(system.expander.io.i2c.interrupts(0))
  io.i2c.scl <> top_gpiov2(system.expander.io.i2c.scl)
  io.i2c.sda <> top_gpiov2(system.expander.io.i2c.sda).setName("fooooo")
  for (index <- 0 until io.gpio.length) {
    io.gpio(index) <> top_gpiov2(system.expander.io.gpio.pins(index))
  }
}
object Sky130Generate extends ElementsApp {
  elementsConfig.genASICSpinalConfig.generateVerilog {
    val top = Sky130Top(I2cGpioExpander.Parameter.default, 128)

    val config = OpenROADTools.Config(elementsConfig)
    config.fixTimingViolations(100)
    config.optimizeSpeed()
    config.generate(OpenROADTools.PDKs.Skywater.sky130hd, (0.0, 0.0, 940.0, 940.0), 250.0)

    val sdc = OpenROADTools.Sdc(elementsConfig)
    sdc.addClock(top.io.clock.PAD, 50 MHz, 0.2)
    sdc.generate()

    val io = OpenROADTools.Io(elementsConfig)
    io.addPad("south", 380, "pwr_io_s1", "sky130_fd_io__top_power_hvc_wpadv2")
    io.addPad("south", 470, "gnd_io_s1", "sky130_fd_io__top_ground_hvc_wpad")
    io.addPad("east", 384, "gnd_core_e1", "sky130_fd_io__top_ground_hvc_wpad")
    io.addPad("east", 479, "pwr_core_e1", "sky130_fd_io__top_power_hvc_wpadv2")
    io.addPad("north", 380, "pwr_io_n1", "sky130_fd_io__top_power_hvc_wpadv2")
    io.addPad("north", 470, "gnd_io_n1", "sky130_fd_io__top_ground_hvc_wpad")
    io.addPad("west", 384, "gnd_core_w1", "sky130_fd_io__top_ground_hvc_wpad")
    io.addPad("west", 479, "pwr_core_w1", "sky130_fd_io__top_power_hvc_wpadv2")
    io.generate(top.io)

    val pdn = OpenROADTools.Pdn(elementsConfig)
    pdn.generate()

    top
  }
}
