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

WORKDIR /opt/elements/
RUN git clone https://github.com/aesc-silicon/elements-container.git
WORKDIR elements-container/
RUN chmod +x container.sh
RUN ./container.sh
