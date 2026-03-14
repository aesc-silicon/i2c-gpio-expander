// SPDX-FileCopyrightText: 2026 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

package i2cgpioexpander
import spinal.core._
import spinal.core.sim._
import spinal.lib._

import nafarr.blackboxes.ihp.sg13cmos5l._
import nafarr.blackboxes.ihp.common._

import zibal.misc.OpenROADTools

import elements.sdk.ElementsApp

case class SG13CMOS5LTop(p: I2cGpioExpander.Parameter, resetDelay: Int) extends Component {
  val io = new Bundle {
    val clock = IhpCmosIo(Edge.South, 0)
    val reset = IhpCmosIo(Edge.South, 1, "clk_core")
    val i2c = new Bundle {
      val scl = IhpCmosIo(Edge.South, 2, "clk_core")
      val sda = IhpCmosIo(Edge.South, 3, "clk_core")
      val interrupt = IhpCmosIo(Edge.South, 4, "clk_core")
    }
    val gpio = Vec(
      IhpCmosIo(Edge.West, 0, "clk_core"),
      IhpCmosIo(Edge.West, 1, "clk_core"),
      IhpCmosIo(Edge.West, 2, "clk_core"),
      IhpCmosIo(Edge.North, 0, "clk_core"),
      IhpCmosIo(Edge.North, 1, "clk_core"),
      IhpCmosIo(Edge.North, 2, "clk_core"),
      IhpCmosIo(Edge.North, 3, "clk_core"),
      IhpCmosIo(Edge.North, 4, "clk_core")
    )
    val address = Vec(
      IhpCmosIo(Edge.East, 2, "clk_core"),
      IhpCmosIo(Edge.East, 3, "clk_core"),
      IhpCmosIo(Edge.East, 4, "clk_core")
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

  val power = Seq(
    IhpPowerIo(Edge.East, 0, IhpPowerIoCell.SG13CMOS5L.Vdd),
    IhpPowerIo(Edge.East, 1, IhpPowerIoCell.SG13CMOS5L.Vss),
    IhpPowerIo(Edge.West, 3, IhpPowerIoCell.SG13CMOS5L.IOVss),
    IhpPowerIo(Edge.West, 4, IhpPowerIoCell.SG13CMOS5L.IOVdd)
  )
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
  chip.ioPower = Some(report.toplevel.power)
  chip.pdnRingWidth = 8.0
  chip.pdnRingSpace = 5.0
  chip.generate
}
