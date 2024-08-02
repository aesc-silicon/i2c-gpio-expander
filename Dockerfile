FROM ubuntu:22.04

ENV DEBIAN_FRONTEND="noninteractive" TZ="Europe/Berlin"

RUN apt-get update && apt-get install -y \
    sudo \
    software-properties-common \
    ssh \
    git \
    curl \
    time \
    libtool-bin \
    autotools-dev \
    automake \
    pkg-config \
    libyaml-dev \
    libssl-dev \
    gdb \
    ninja-build \
    flex \
    bison \
    libfl-dev \
    cmake \
    libftdi1-dev \
    python3.10 \
    python3.10-dev \
    python3-pip \
    libpython3.10 \
    virtualenv \
    openjdk-11-jdk-headless \
    verilator \
    gtkwave \
    libcanberra-gtk-module \
    libcanberra-gtk3-module \
    libtinfo5 \
    libncurses5 \
    klayout

RUN add-apt-repository ppa:deadsnakes/ppa
RUN apt-get update && apt-get install -y \
    python3.9 \
    python3.9-dev \
    python3-pip \
    libpython3.9

RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | gpg --no-default-keyring --keyring gnupg-ring:/etc/apt/trusted.gpg.d/scalasbt-release.gpg --import
RUN chmod 644 /etc/apt/trusted.gpg.d/scalasbt-release.gpg
RUN apt-get update && apt-get install -y sbt

# Zephyr SDK

ARG ZEPHYR_SDK_RELEASE=0.16.5

WORKDIR /opt/elements/

RUN wget https://github.com/zephyrproject-rtos/sdk-ng/releases/download/v${ZEPHYR_SDK_RELEASE}/zephyr-sdk-${ZEPHYR_SDK_RELEASE}_linux-x86_64.tar.xz && \
    wget -O - https://github.com/zephyrproject-rtos/sdk-ng/releases/download/v${ZEPHYR_SDK_RELEASE}/sha256.sum | shasum --check --ignore-missing && \
    tar xvf zephyr-sdk-${ZEPHYR_SDK_RELEASE}_linux-x86_64.tar.xz && \
    rm zephyr-sdk-${ZEPHYR_SDK_RELEASE}_linux-x86_64.tar.xz

# OSS Cad Suite

ARG OSS_CAD_SUITE_YEAR=2024
ARG OSS_CAD_SUITE_MONTH=07
ARG OSS_CAD_SUITE_DAY=17
ARG OSS_CAD_SUITE_DATE="${OSS_CAD_SUITE_YEAR}-${OSS_CAD_SUITE_MONTH}-${OSS_CAD_SUITE_DAY}"
ARG OSS_CAD_SUITE_STAMP="${OSS_CAD_SUITE_YEAR}${OSS_CAD_SUITE_MONTH}${OSS_CAD_SUITE_DAY}"

WORKDIR /opt/elements/

RUN wget https://github.com/YosysHQ/oss-cad-suite-build/releases/download/${OSS_CAD_SUITE_DATE}/oss-cad-suite-linux-x64-${OSS_CAD_SUITE_STAMP}.tgz && \
    tar -xvf oss-cad-suite-linux-x64-${OSS_CAD_SUITE_STAMP}.tgz && \
    rm oss-cad-suite-linux-x64-${OSS_CAD_SUITE_STAMP}.tgz

# OpenROAD, KLayout, OpenROAD flow scripts

ARG OPENROAD_YEAR=2024
ARG OPENROAD_MONTH=07
ARG OPENROAD_DAY=17
ARG OPENROAD_VERSION="${OPENROAD_YEAR}-${OPENROAD_MONTH}-${OPENROAD_DAY}"
ARG KLAYOUT_VERSION=0.29.0
ARG OPENROAD_FLOW_ORGA=dnltz
ARG OPENROAD_FLOW_COMMIT=53a9b78f1f8e851c45d5e3cfa90e3bd854ca0cd7

WORKDIR /opt/elements/

RUN wget https://github.com/Precision-Innovations/OpenROAD/releases/download/${OPENROAD_VERSION}/openroad_2.0_amd64-ubuntu20.04-${OPENROAD_VERSION}.deb && \
    sudo apt install -y ./openroad_2.0_amd64-ubuntu20.04-${OPENROAD_VERSION}.deb && \
    rm openroad_2.0_amd64-ubuntu20.04-${OPENROAD_VERSION}.deb

RUN wget https://www.klayout.org/downloads/Ubuntu-22/klayout_${KLAYOUT_VERSION}-1_amd64.deb && \
    sudo apt install -y ./klayout_${KLAYOUT_VERSION}-1_amd64.deb && \
    rm klayout_${KLAYOUT_VERSION}-1_amd64.deb

WORKDIR /opt/elements/tools

RUN git clone --progress https://github.com/${OPENROAD_FLOW_ORGA}/OpenROAD-flow-scripts.git && \
    cd OpenROAD-flow-scripts && \
    git checkout ${OPENROAD_FLOW_COMMIT}

# IHP Open PDK

ARG IHP_PDK_VERSION=5a42d03194e8c98558f4e34538338a60550f89b9

WORKDIR /opt/elements/pdks

RUN git clone --progress https://github.com/IHP-GmbH/IHP-Open-PDK.git && \
    cd IHP-Open-PDK && \
    git checkout ${IHP_PDK_VERSION}

WORKDIR /opt/elements/
