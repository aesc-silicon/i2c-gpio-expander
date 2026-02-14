// SPDX-FileCopyrightText: 2026 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

package i2cgpioexpander
import spinal.core._
import spinal.core.sim._
import spinal.lib._

import nafarr.blackboxes.ihp.sg13cmos5l._

import zibal.misc.OpenROADTools

import elements.sdk.ElementsApp

case class SG13CMOS5LTop(p: I2cGpioExpander.Parameter, resetDelay: Int) extends Component {
  val io = new Bundle {
    val clock = IhpCmosIo("south", 0)
    val reset = IhpCmosIo("south", 1, "clk_core")
    val i2c = new Bundle {
      val scl = IhpCmosIo("south", 2, "clk_core")
      val sda = IhpCmosIo("south", 3, "clk_core")
      val interrupt = IhpCmosIo("south", 4, "clk_core")
    }
    val gpio = Vec(
      IhpCmosIo("west", 0, "clk_core"),
      IhpCmosIo("west", 1, "clk_core"),
      IhpCmosIo("west", 2, "clk_core"),
      IhpCmosIo("north", 0, "clk_core"),
      IhpCmosIo("north", 1, "clk_core"),
      IhpCmosIo("north", 2, "clk_core"),
      IhpCmosIo("north", 3, "clk_core"),
      IhpCmosIo("north", 4, "clk_core")
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
object SG13CMOS5LGenerate extends ElementsApp {
  val report = elementsConfig.genASICSpinalConfig.generateVerilog {
    val top = SG13CMOS5LTop(I2cGpioExpander.Parameter.default.copy(addressWidth = 3), 128)
    top
  }

  val chip = OpenROADTools.IHP.Config(elementsConfig, OpenROADTools.PDKs.IHP.sg13cmos5l)
  chip.dieArea = (0, 0, 1050.24, 1050.84)
  chip.coreArea = (351.36, 351.54, 699.84, 699.3)
  chip.hasIoRing = true
  chip.addClock(report.toplevel.io.clock.PAD, 50 MHz, "clk_core")
  chip.io = Some(report.toplevel.io)
  chip.pdnRingWidth = 8.0
  chip.pdnRingSpace = 5.0
  chip.addPad("east", 0, "sg13cmos5l_IOPadVdd")
  chip.addPad("east", 1, "sg13cmos5l_IOPadVss")
  chip.addPad("west", 3, "sg13cmos5l_IOPadIOVss")
  chip.addPad("west", 4, "sg13cmos5l_IOPadIOVdd")
  chip.generate
}
