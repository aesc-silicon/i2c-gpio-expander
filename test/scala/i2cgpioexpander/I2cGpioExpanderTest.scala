package i2cgpioexpander

import org.scalatest.funsuite.AnyFunSuite

import spinal.sim._
import spinal.core._
import spinal.core.sim._

import nafarr.peripherals.com.i2c.I2cControllerSim

class I2cDeviceCtrlTest extends AnyFunSuite {
  test("basic") {
    val compiled = SimConfig.withWave.compile {
      val cd = ClockDomain.current.copy(frequency = FixedFrequency(50 MHz))
      val area = new ClockingArea(cd) {
        val dut = I2cGpioExpander(I2cGpioExpander.Parameter.default)
      }
      area.dut
    }
    compiled.doSim("wrongDeviceAddrNack") { dut =>
      dut.clockDomain.forkStimulus(20 * 1000)
      fork {
        dut.clockDomain.fallingEdge()
        sleep(20 * 1000)
        while (true) {
          dut.clockDomain.clockToggle()
          sleep(10 * 1000)
        }
      }
      val baudrate = 2500
      val tickPeriod = baudrate / 4

      dut.io.address #= BigInt("001", 2)

      dut.clockDomain.assertReset()
      sleep(100 * 1000)
      dut.clockDomain.deassertReset()

      val start = I2cControllerSim.start(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      start.join()

      val addr = I2cControllerSim.address(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod,
                                          BigInt("0000110", 2), false)
      addr.join()

      val addrNack = I2cControllerSim.checkNack(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      addrNack.join()

      sleep(1000 * 1000)
    }

    compiled.doSim("readReg1") { dut =>
      dut.clockDomain.forkStimulus(20 * 1000)
      fork {
        dut.clockDomain.fallingEdge()
        sleep(20 * 1000)
        while (true) {
          dut.clockDomain.clockToggle()
          sleep(10 * 1000)
        }
      }
      val baudrate = 2500
      val tickPeriod = baudrate / 4

      dut.io.gpio.pins.read #= BigInt("01101010", 2)
      dut.io.address #= BigInt("000", 2)

      dut.clockDomain.assertReset()
      sleep(100 * 1000)
      dut.clockDomain.deassertReset()

      val start = I2cControllerSim.start(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      start.join()

      val addr = I2cControllerSim.writeByte(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod,
                                            BigInt("00000110", 2))
      addr.join()

      val addrAck = I2cControllerSim.checkAck(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      addrAck.join()

      val reg = I2cControllerSim.writeByte(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod,
                                           BigInt("00000000", 2))
      reg.join()

      val regAck = I2cControllerSim.checkAck(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      regAck.join()

      val start2 = I2cControllerSim.start(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      start2.join()

      val addr2 = I2cControllerSim.writeByte(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod,
                                            BigInt("10000110", 2))
      addr2.join()

      val addr2Ack = I2cControllerSim.checkAck(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      addr2Ack.join()

      val read = I2cControllerSim.readByte(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod,
                                           BigInt("10101001", 2))
      read.join()

      val readAck = I2cControllerSim.sendAck(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      readAck.join()

      val stop2 = I2cControllerSim.stop(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      stop2.join()

      sleep(10000 * 1000)
    }

    compiled.doSim("writeReg0") { dut =>
      dut.clockDomain.forkStimulus(20 * 1000)
      fork {
        dut.clockDomain.fallingEdge()
        sleep(20 * 1000)
        while (true) {
          dut.clockDomain.clockToggle()
          sleep(10 * 1000)
        }
      }
      val baudrate = 2500
      val tickPeriod = baudrate / 4

      dut.io.address #= BigInt("000", 2)

      dut.clockDomain.assertReset()
      sleep(100 * 1000)
      dut.clockDomain.deassertReset()

      val start = I2cControllerSim.start(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      start.join()

      val addr = I2cControllerSim.writeByte(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod,
                                            BigInt("00000110", 2))
      addr.join()

      val addrAck = I2cControllerSim.checkAck(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      addrAck.join()

      val reg = I2cControllerSim.writeByte(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod,
                                           BigInt("00000000", 2))
      reg.join()

      val regAck = I2cControllerSim.checkAck(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      sleep(100 * 1000)
      regAck.join()

      val write = I2cControllerSim.writeByte(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod,
                                           BigInt("01010110", 2))
      write.join()

      val writenNack = I2cControllerSim.checkNack(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      writenNack.join()

      val stop = I2cControllerSim.stop(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      stop.join()

      sleep(10000 * 1000)
    }

    compiled.doSim("writeReg1") { dut =>
      dut.clockDomain.forkStimulus(20 * 1000)
      fork {
        dut.clockDomain.fallingEdge()
        sleep(20 * 1000)
        while (true) {
          dut.clockDomain.clockToggle()
          sleep(10 * 1000)
        }
      }
      val baudrate = 2500
      val tickPeriod = baudrate / 4

      dut.io.address #= BigInt("000", 2)

      dut.clockDomain.assertReset()
      sleep(100 * 1000)
      dut.clockDomain.deassertReset()

      val start = I2cControllerSim.start(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      start.join()

      val addr = I2cControllerSim.writeByte(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod,
                                            BigInt("00000110", 2))
      addr.join()

      val addrAck = I2cControllerSim.checkAck(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      addrAck.join()

      val reg = I2cControllerSim.writeByte(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod,
                                           BigInt("10000000", 2))
      reg.join()

      val regAck = I2cControllerSim.checkAck(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      sleep(100 * 1000)
      regAck.join()

      val write = I2cControllerSim.writeByte(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod,
                                           BigInt("01010110", 2))
      write.join()

      val writeAck = I2cControllerSim.checkAck(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      writeAck.join()

      val stop = I2cControllerSim.stop(dut.io.i2c.sda, dut.io.i2c.scl, tickPeriod)
      stop.join()

      sleep(10000 * 1000)
    }
  }
}
