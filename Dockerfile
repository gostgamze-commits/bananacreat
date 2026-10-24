FROM ubuntu:latest

# Install Java and Android SDK tools
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    wget \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# Set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# Install Android SDK
RUN mkdir -p /android-sdk && cd /android-sdk && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip && \
    unzip -q commandlinetools-linux-9477386_latest.zip && \
    rm commandlinetools-linux-9477386_latest.zip

ENV ANDROID_SDK_ROOT=/android-sdk
ENV PATH=$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH

# Accept licenses and install SDK components
RUN yes | sdkmanager --sdk_root=$ANDROID_SDK_ROOT \
    "platforms;android-34" \
    "build-tools;34.0.0" \
    "cmdline-tools;latest" \
    2>&1 | grep -v "Warning" || true

WORKDIR /project

# Copy project
COPY . .

# Build APK
RUN chmod +x gradlew && \
    ./gradlew assembleDebug --no-daemon

# Output APK to volume mount
CMD ["cp", "-v", "app/build/outputs/apk/debug/app-debug.apk", "/output/"]
