# Use JDK 17 (Ubuntu Jammy based)
FROM eclipse-temurin:17-jdk-jammy

# Set environment variables
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_HOME=/opt/android-sdk
ENV GRADLE_USER_HOME=/home/osmand/.gradle
ENV PATH=${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools

# Install necessary packages
RUN apt-get update && apt-get install -y \
    git \
    curl \
    unzip \
    build-essential \
    python3 \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && npm install -g @google/gemini-cli \
    && rm -rf /var/lib/apt/lists/*

# Install Android SDK Command Line Tools
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools && \
    curl -o /tmp/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip && \
    unzip /tmp/cmdline-tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools && \
    mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Accept licenses
RUN yes | sdkmanager --licenses

# Install SDK components
RUN sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools" "ndk;26.1.10909125" "cmake;3.22.1"

# Create group and user
RUN groupadd -g 1000 osmand && \
    useradd -u 1000 -g 1000 -o -m -s /bin/bash osmand

# Set up the working directory and ensure permissions
RUN mkdir -p /workspace ${GRADLE_USER_HOME} && \
    chown -R osmand:osmand /workspace ${ANDROID_SDK_ROOT} ${GRADLE_USER_HOME}

# Pre-download Gradle distribution
WORKDIR /tmp/gradle-setup
COPY gradlew .
COPY gradle/ gradle/
RUN chown -R osmand:osmand /tmp/gradle-setup
USER osmand
RUN ./gradlew --version && rm -rf /tmp/gradle-setup

WORKDIR /workspace

# Default command to build the project
# CMD ["./gradlew", "assembleDebug"]2
