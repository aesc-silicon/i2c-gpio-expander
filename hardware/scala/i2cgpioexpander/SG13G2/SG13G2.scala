// SPDX-FileCopyrightText: 2025 aesc silicon
//
// SPDX-License-Identifier: CERN-OHL-W-2.0

package i2cgpioexpander
import spinal.core._
import spinal.core.sim._
import spinal.lib._

import nafarr.blackboxes.ihp.sg13g2._
import nafarr.blackboxes.ihp.common._

import zibal.misc.OpenROADTools

import elements.sdk.ElementsApp

case class SG13G2Top(p: I2cGpioExpander.Parameter, resetDelay: Int) extends Component {
  val io = new Bundle {
    val clock = IhpCmosIo(Edge.South, 0)
    val reset = IhpCmosIo(Edge.South, 1, "clk_core")
    val i2c = new Bundle {
      val scl = IhpCmosIo(Edge.South, 2, "clk_core")
      val sda = IhpCmosIo(Edge.South, 3, "clk_core")
      val interrupt = IhpCmosIo(Edge.South, 4, "clk_core")
    }
    val gpio = Vec(
      IhpCmosIo(Edge.West, 2, "clk_core"),
      IhpCmosIo(Edge.West, 3, "clk_core"),
      IhpCmosIo(Edge.West, 4, "clk_core"),
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
    IhpPowerIo(Edge.East, 0, IhpPowerIoCell.SG13G2.Vdd),
    IhpPowerIo(Edge.East, 1, IhpPowerIoCell.SG13G2.Vss),
    IhpPowerIo(Edge.West, 0, IhpPowerIoCell.SG13G2.IOVdd),
    IhpPowerIo(Edge.West, 1, IhpPowerIoCell.SG13G2.IOVss)
  )
}
object SG13G2Generate extends ElementsApp {
  val report = elementsConfig.genASICSpinalConfig.generateVerilog {
    val top = SG13G2Top(I2cGpioExpander.Parameter.default.copy(addressWidth = 3), 128)
    top
  }

  val i2cDevice = OpenROADTools.IHP.Config(elementsConfig, OpenROADTools.PDKs.IHP.sg13g2, true)
  i2cDevice.dieArea = (0, 0, 147.84, 147.42)
  i2cDevice.coreArea = (15.36, 15.12, 132, 132.3)
  i2cDevice.pdnRingWidth = 3.0
  i2cDevice.pdnRingSpace = 2.0
  i2cDevice.addClock(report.toplevel.clockCtrl.mainClockDomain.clock, 50 MHz)
  i2cDevice.generate("I2cDeviceCtrl")

  val chip = OpenROADTools.IHP.Config(elementsConfig, OpenROADTools.PDKs.IHP.sg13g2)
  chip.dieArea = (0, 0, 1050.24, 1050.84)
  chip.coreArea = (351.36, 351.54, 699.84, 699.3)
  chip.hasIoRing = true
  chip.addBlock(report.toplevel.system.expander.i2cCtrl, "I2cDeviceCtrl", 351.7, 351.94)
  chip.addClock(report.toplevel.io.clock.PAD, 50 MHz, "clk_core")
  chip.io = Some(report.toplevel.io)
  chip.ioPower = Some(report.toplevel.power)
  chip.pdnRingWidth = 8.0
  chip.pdnRingSpace = 5.0
  chip.generate
}
