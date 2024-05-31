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
PDK_SG13G2_KLAYOUT_DIR=${PWD}/pdks/IHP-Open-PDK/ihp-sg13g2/libs.tech/klayout/

sg13g2-klayout: KLAYOUT_HOME=${PDK_SG13G2_KLAYOUT_DIR}
sg13g2-drc-minimal: KLAYOUT_HOME=${PDK_SG13G2_KLAYOUT_DIR}
sg13g2-drc-minimal-gui: KLAYOUT_HOME=${PDK_SG13G2_KLAYOUT_DIR}
sg13g2-drc-maximal: KLAYOUT_HOME=${PDK_SG13G2_KLAYOUT_DIR}
sg13g2-drc-maximal-gui: KLAYOUT_HOME=${PDK_SG13G2_KLAYOUT_DIR}

# Commands for FPGA design verification
ecp5-generate:
	BOARD=ECPIX5 sbt "runMain i2cgpioexpander.boards.ECPIX5Generate"

ecp5-synthesize: ecp5-generate
	mkdir -p ${BUILD_ROOT}/${SOC}/ECPIX5/fpga/
	BOARD=ECPIX5 ./modules/elements/zibal/eda/Lattice/fpga/syn.sh

ecp5-flash:
	openFPGALoader -b ${OPENFPGALOADER_BOARD} ${BUILD_ROOT}/${SOC}/ECPIX5/fpga/ECPIX5Top.bit


# Commands for SKY130 tape-out
sky130-generate:
	BOARD=Sky130 sbt "runMain i2cgpioexpander.boards.Sky130Generate"

sky130-synthesize:
	source ${OPENROAD_FLOW_ROOT}/../env.sh && make -C ${OPENROAD_FLOW_ROOT} DESIGN_CONFIG=${BUILD_ROOT}/${SOC}/Sky130/zibal/Sky130Top.mk

sky130-openroad:
	openroad -gui <(echo read_db ${OPENROAD_FLOW_ROOT}/results/sky130hd/Sky130Top/base/6_final.odb)

sky130-magic:
	magic -T sky130B


# Commands for SG13G2 tape-out
sg13g2-generate:
	BOARD=SG13G2 sbt "runMain i2cgpioexpander.boards.SG13G2Generate"

sg13g2-synthesize:
	source ${OPENROAD_FLOW_ROOT}/../env.sh && make -C ${OPENROAD_FLOW_ROOT} DESIGN_CONFIG=${BUILD_ROOT}/${SOC}/SG13G2/zibal/SG13G2Top.mk

sg13g2-openroad:
	openroad -gui <(echo read_db ${OPENROAD_FLOW_ROOT}/results/ihp-sg13g2/SG13G2Top/base/6_final.odb)

sg13g2-klayout:
	klayout -e ${OPENROAD_FLOW_ROOT}/results/ihp-sg13g2/SG13G2Top/base/6_final.gds

sg13g2-drc-minimal:
	klayout -b -r ${KLAYOUT_HOME}/tech/drc/sg13g2_minimal.lydrc  -rd cell=SG13G2Top ${OPENROAD_FLOW_ROOT}/results/ihp-sg13g2/SG13G2Top/base/6_final.gds

sg13g2-drc-minimal-gui:
	klayout -e ${OPENROAD_FLOW_ROOT}/results/ihp-sg13g2/SG13G2Top/base/6_final.gds -m ${KLAYOUT_HOME}/tech/drc/sg13g2_minimal.lyrdb

sg13g2-drc-maximal:
	klayout -b -r ${KLAYOUT_HOME}/tech/drc/sg13g2_maximal.lydrc  -rd cell=SG13G2Top ${OPENROAD_FLOW_ROOT}/results/ihp-sg13g2/SG13G2Top/base/6_final.gds

sg13g2-drc-maximal-gui:
	klayout -e ${OPENROAD_FLOW_ROOT}/results/ihp-sg13g2/SG13G2Top/base/6_final.gds -m ${KLAYOUT_HOME}/tech/drc/sg13g2_maximal.lyrdb

# Misc.
clean:
	rm -rf build/
