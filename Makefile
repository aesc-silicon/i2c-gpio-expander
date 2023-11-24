SHELL := /bin/bash

.EXPORT_ALL_VARIABLES:

PATH=${PWD}/oss-cad-suite/bin/:${PWD}/tools/magic/build/bin/:$(shell printenv PATH)
BUILD_ROOT=${PWD}/build/
OPENROAD_FLOW_ROOT=${PWD}/tools/OpenROAD-flow-scripts/flow
SOC=I2cGpioExpander
FPGA_FAMILY=ecp5
FPGA_DEVICE=um5g-45k
FPGA_PACKAGE=CABGA554
FPGA_FREQUENCY=50
OPENFPGALOADER_BOARD=ecpix5_r03
CAD_ROOT=${PWD}/tools/magic/build/lib/
OPENROAD_EXE=/usr/bin/openroad
YOSYS_CMD=${PWD}/oss-cad-suite/bin/yosys
PDK_SKY130_IO_DIR=${PWD}/pdks/share/pdk/sky130A/libs.ref/sky130_fd_io/

OSS_CAD_SUITE_DATE="2023-10-30"
OSS_CAD_SUITE_STAMP="${OSS_CAD_SUITE_DATE//-}"

# Commands for SKY130 tape-out
open-magic:
	magic -T sky130B

sky130-generate:
	BOARD=Sky130 sbt "runMain i2cgpioexpander.boards.Sky130Generate"

sky130-synthesize:
	source ${OPENROAD_FLOW_ROOT}/../env.sh && make -C ${OPENROAD_FLOW_ROOT} DESIGN_CONFIG=${BUILD_ROOT}/${SOC}/Sky130/zibal/Sky130Top.mk

sky130-gui:
	openroad -gui <(echo read_db ${OPENROAD_FLOW_ROOT}/results/sky130hd/Sky130Top/base/6_final.odb)

# Commands for FPGA design verification
ecp5-generate:
	BOARD=ECPIX5 sbt "runMain i2cgpioexpander.boards.ECPIX5Generate"

ecp5-synthesize: ecp5-generate
	mkdir -p ${BUILD_ROOT}/${SOC}/ECPIX5/fpga/
	BOARD=ECPIX5 ./modules/elements/zibal/eda/Lattice/fpga/syn.sh

ecp5-flash:
	openFPGALoader -b ${OPENFPGALOADER_BOARD} ${BUILD_ROOT}/${SOC}/ECPIX5/fpga/ECPIX5Top.bit

# Misc.
clean:
	rm -rf build/
