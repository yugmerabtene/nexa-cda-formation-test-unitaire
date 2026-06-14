FROM maven:3.9-eclipse-temurin-17

ARG USER_HOME_DIR="/root"

RUN apt-get update && apt-get install -y \
    git \
    curl \
    unzip \
    && rm -rf /var/lib/apt/lists/*

RUN curl -sL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /root/.m2

COPY settings.xml /root/.m2/settings.xml

WORKDIR /workspace

CMD ["bash"]
