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
import zibal.misc.TestCases

import elements.sdk.ElementsApp

case class SG13G2Board() extends Component {
  val io = new Bundle {
    val clock = inout(Analog(Bool))
    val reset = inout(Analog(Bool))
    val gpio = Vec(inout(Analog(Bool)), 8)
    val i2c = new Bundle {
      val scl = inout(Analog(Bool))
      val sda = inout(Analog(Bool))
      val interrupt = inout(Analog(Bool))
    }
  }

  val top = SG13G2Top(I2cGpioExpander.Parameter.default.copy(addressWidth = 3), 128)
  val analogFalse = Analog(Bool)
  analogFalse := False
  val analogTrue = Analog(Bool)
  analogTrue := True

  top.io.clock.PAD := io.clock
  top.io.reset.PAD := io.reset

  io.i2c.scl <> top.io.i2c.scl.PAD
  io.i2c.sda <> top.io.i2c.sda.PAD
  io.i2c.interrupt <> top.io.i2c.interrupt.PAD

  for (index <- 0 until top.io.address.length) {
    top.io.address(index).PAD := analogFalse
  }

  for (index <- 0 until top.io.gpio.length) {
    io.gpio(index) <> top.io.gpio(index).PAD
  }
}

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

object SG13G2Simulate extends ElementsApp {
  val compiled = elementsConfig.genFPGASimConfig.compile {
    val board = SG13G2Board()
    board.top.system.expander.io.i2c.sda.simPublic()
    board
  }
  simType match {
    case "simulate" =>
      compiled.doSimUntilVoid("simulate") { dut =>
        val testCases = TestCases()
        val baudrate = 2500
        val tickPeriod = baudrate / 4
        val data: Seq[(BigInt, BigInt)] = List(
          (BigInt("10000000", 2), BigInt("01010101", 2)),
          (BigInt("01000000", 2), BigInt("00010101", 2))
        )

        testCases.addClock(
          dut.io.clock,
          dut.top.clockCtrl.mainClockDomain.frequency.getValue,
          simDuration.toString.toInt ms
        )
        testCases.addReset(dut.io.reset, 100 us)
        testCases.i2cDeviceWrite(
          dut.io.i2c.sda,
          dut.top.system.expander.io.i2c.sda,
          dut.io.i2c.scl,
          tickPeriod,
          110 us,
          BigInt("0000110", 2),
          data
        )
      }
    case "write" =>
      compiled.doSimUntilVoid("simulate") { dut =>
        val testCases = TestCases()
        val baudrate = 2500
        val tickPeriod = baudrate / 4
        val data: Seq[(BigInt, BigInt)] = List(
          (BigInt("10000000", 2), BigInt("01010101", 2)),
          (BigInt("01000000", 2), BigInt("00010101", 2))
        )

        testCases.addClock(
          dut.io.clock,
          dut.top.clockCtrl.mainClockDomain.frequency.getValue,
          simDuration.toString.toInt ms
        )
        testCases.addReset(dut.io.reset, 100 us)
        testCases.i2cDeviceWrite(
          dut.io.i2c.sda,
          dut.top.system.expander.io.i2c.sda,
          dut.io.i2c.scl,
          tickPeriod,
          110 us,
          BigInt("0000110", 2),
          data
        )
        testCases.gpioCheckStates(dut.io.gpio, BigInt("10101000", 2), 300 us)
      }
    case "read" =>
      compiled.doSimUntilVoid("simulate") { dut =>
        val testCases = TestCases()
        val baudrate = 2500
        val tickPeriod = baudrate / 4

        fork {
          while (true) {
            dut.io.gpio(3) #= true
            dut.io.gpio(5) #= true
            dut.io.gpio(7) #= true
            sleep(5 * 1000)
          }
        }

        testCases.addClock(
          dut.io.clock,
          dut.top.clockCtrl.mainClockDomain.frequency.getValue,
          simDuration.toString.toInt ms
        )
        testCases.addReset(dut.io.reset, 100 us)
        dut.io.gpio(0) #= true
        testCases.i2cDeviceRead(
          dut.io.i2c.sda,
          dut.top.system.expander.io.i2c.sda,
          dut.io.i2c.scl,
          tickPeriod,
          110 us,
          BigInt("0000110", 2),
          BigInt("00000000", 2),
          BigInt("00010101", 2)
        )
      }
  }
}
