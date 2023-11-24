#!/bin/bash

PATH=${PWD}/oss-cad-suite/bin/:${PWD}/tools/magic/build/bin/:$PATH
OSS_CAD_SUITE_DATE="2023-10-30"
OSS_CAD_SUITE_STAMP="${OSS_CAD_SUITE_DATE//-}"

MAGIC_VERSION=8.3.453
SKY130_VERSION=1.0.457
OPENROAD_VERSION=2023-11-17
OPENROAD_FLOW_VERSION=97b4db7

function fetch_elements {
	mkdir -p modules/elements
	cd modules/elements/
	git clone git@github.com:SpinalHDL/SpinalCrypto.git -b 27e0ceb430ac
	git clone git@github.com:aesc-silicon/elements-nafarr.git nafarr -b 281930a5e879
	git clone git@github.com:aesc-silicon/elements-zibal.git zibal -b ec5c139b4fe6
	git clone git@github.com:aesc-silicon/elements-vexriscv.git vexriscv -b 1ea2027464a1
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

function install_sky130 {
	mkdir -p pdks
	cd pdks/
	git clone https://github.com/RTimothyEdwards/open_pdks -b ${SKY130_VERSION}
	cd open_pdks
	./configure --enable-sky130-pdk make --prefix=${PWD}/../
	make -j$(nproc)
	make install
	cd ../share/pdk/
	# Fix errors in sky130_ef_io LEF files
	sed -i 's/END sky130_ef_io__analog_pad/END sky130_ef_io__analog_esd_pad/g' sky130A/libs.ref/sky130_fd_io/lef/sky130_ef_io.lef
	sed -i '1000,1100s/END sky130_ef_io__analog_noesd_pad/END sky130_ef_io__analog_pad/g' sky130A/libs.ref/sky130_fd_io/lef/sky130_ef_io.lef
	sed -i 's/END sky130_ef_io__analog_pad/END sky130_ef_io__analog_esd_pad/g' sky130B/libs.ref/sky130_fd_io/lef/sky130_ef_io.lef
	sed -i '1000,1100s/END sky130_ef_io__analog_noesd_pad/END sky130_ef_io__analog_pad/g' sky130B/libs.ref/sky130_fd_io/lef/sky130_ef_io.lef
	ln -sf ${PWD}/sky130B/libs.tech/magic/* ${PWD}/../../../tools/magic/build/lib/magic/sys/
	cd ../../../
}

function install_openroad {
	cd tools
	wget https://github.com/Precision-Innovations/OpenROAD/releases/download/${OPENROAD_VERSION}/openroad_2.0_amd64-ubuntu22.04-${OPENROAD_VERSION}.deb
	sudo apt install ./openroad_2.0_amd64-ubuntu22.04-${OPENROAD_VERSION}.deb
	rm ./openroad_2.0_amd64-ubuntu22.04-${OPENROAD_VERSION}.deb
	git clone https://github.com/The-OpenROAD-Project/OpenROAD-flow-scripts.git
	cd OpenROAD-flow-scripts/
	git checkout ${OPENROAD_FLOW_VERSION}
	git submodule update --init --recursive
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
	echo "init.sh [-h]"
	echo "\t-h: Show this help message"
}

while getopts h flag
do
	case "${flag}" in
		h) print_usage
			exit 1;;
	esac
done

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
	install_sky130
fi
if ! test -d "tools/OpenROAD-flow-scripts"; then
	install_openroad
fi
if ! test -d "tools/gdsiistl"; then
	install_gdsiistl
fi
