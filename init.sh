#!/bin/bash

PATH=${PWD}/oss-cad-suite/bin/:${PWD}/tools/magic/build/bin/:$PATH

OSS_CAD_SUITE_DATE="2024-05-26"
OSS_CAD_SUITE_STAMP="${OSS_CAD_SUITE_DATE//-}"
OPENROAD_VERSION=2024-05-26
OPENROAD_FLOW_ORGA=dnltz
OPENROAD_FLOW_VERSION=2ff42a33f0504e4f8660940e5574704f9a342a60
KLAYOUT_VERSION=0.29.0

MAGIC_VERSION=8.3.453
SKY130_VERSION=1.0.485

NAFARR_VERSION=bfc54d0d612cffd3c2c2a36bbbb825be3eff0e88
ZIBAL_VERSION=20b04fd5496022f4127f0cc651137732c7ba2279

function fetch_elements {
	mkdir -p modules/elements
	cd modules/elements/
	git clone git@github.com:SpinalHDL/SpinalCrypto.git
	cd SpinalCrypto
	git checkout 27e0ceb430ac
	cd ../
	git clone git@github.com:aesc-silicon/elements-nafarr.git nafarr
	cd nafarr
	git checkout ${NAFARR_VERSION}
	cd ../
	git clone git@github.com:aesc-silicon/elements-zibal.git zibal
	cd zibal
	git checkout ${ZIBAL_VERSION}
	cd ../
	git clone git@github.com:aesc-silicon/elements-vexriscv.git vexriscv
	cd vexriscv
	git checkout 15e5d08322ef
	cd ../
	cd ../../
}

function fetch_oss_cad_suite_build {
	wget https://github.com/YosysHQ/oss-cad-suite-build/releases/download/${OSS_CAD_SUITE_DATE}/oss-cad-suite-linux-x64-${OSS_CAD_SUITE_STAMP}.tgz
	tar -xvf oss-cad-suite-linux-x64-${OSS_CAD_SUITE_STAMP}.tgz
	rm oss-cad-suite-linux-x64-${OSS_CAD_SUITE_STAMP}.tgz
}

function install_magic {
	mkdir -p tools
	cd tools
	git clone https://github.com/RTimothyEdwards/magic -b ${MAGIC_VERSION}
	cd magic/
	./configure --prefix=${PWD}/build/
	make -j$(nproc)
	make install
	cd ../../
}

function install_sg13g2 {
	mkdir -p pdks
	cd pdks
	git clone git@github.com:IHP-GmbH/IHP-Open-PDK.git
	cd IHP-Open-PDK
	git checkout -t origin/dev
	cd ../../
}

function install_sky130 {
	mkdir -p pdks
	cd pdks/
	git clone https://github.com/RTimothyEdwards/open_pdks -b ${SKY130_VERSION}
	cd open_pdks
	./configure --enable-sky130-pdk make --prefix=${PWD}/../
	make -j$(nproc)
	make install
	cd ../share/pdk/
	ln -sf ${PWD}/sky130B/libs.tech/magic/* ${PWD}/../../../tools/magic/build/lib/magic/sys/
	cd ../../../
}

function install_openroad {
	cd tools
	wget https://github.com/Precision-Innovations/OpenROAD/releases/download/${OPENROAD_VERSION}/openroad_2.0_amd64-ubuntu22.04-${OPENROAD_VERSION}.deb
	sudo apt install ./openroad_2.0_amd64-ubuntu22.04-${OPENROAD_VERSION}.deb
	wget https://www.klayout.org/downloads/Ubuntu-22/klayout_${KLAYOUT_VERSION}-1_amd64.deb
	sudo apt install -y ./klayout_${KLAYOUT_VERSION}-1_amd64.deb
	rm ./*.deb
	git clone https://github.com/${OPENROAD_FLOW_ORGA}/OpenROAD-flow-scripts.git
	cd OpenROAD-flow-scripts/
	git checkout ${OPENROAD_FLOW_VERSION}
	git submodule update --init --recursive --progress
	cd ../../
}

function install_gdsiistl {
	cd tools
	git clone https://github.com/aesc-silicon/gdsiistl.git
	cd gdsiistl
	virtualenv venv
	source venv/bin/activate
	pip3 install -r requirements.txt
	cd ../../
}

function print_usage {
	echo "init.sh [-h] [sg13g2/sky130]"
	echo "\t-h: Show this help message"
	echo "\tsg13g2: Download IHP SG13G2 PDK"
	echo "\tsky130: Download SkyWater SKY130 PDK"
}

while getopts h flag
do
	case "${flag}" in
		h) print_usage
			exit 1;;
	esac
done

sg13g2=false
sky130=false
case $1 in
	sg13g2)
		sg13g2=true;;
	sky130)
		sky130=true;;
esac

if ! test -d "modules/elements"; then
	fetch_elements
fi
if ! test -d "oss-cad-suite"; then
	fetch_oss_cad_suite_build
fi
if ! test -d "tools/magic"; then
	install_magic
fi
if ! test -d "pdks/share/pdk/sky130B"; then
	if [ "$sky130" = true ]; then
		install_sky130
	else
		echo "Skipped downloading SKY130 PDK."
	fi
fi
if ! test -d "pdks/IHP-Open-PDK"; then
	if [ "$sg13g2" = true ]; then
		install_sg13g2
	else
		echo "Skipped downloading SK13G2 PDK."
	fi
fi
if ! test -d "tools/OpenROAD-flow-scripts"; then
	install_openroad
fi
if ! test -d "tools/gdsiistl"; then
	install_gdsiistl
fi
