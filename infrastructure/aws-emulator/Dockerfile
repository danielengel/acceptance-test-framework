FROM public.ecr.aws/amazonlinux/amazonlinux:2023
ARG TARGETARCH
ARG EMULATOR_VERSION=1.0.385
ARG EMULATOR_PACKAGE_URL
COPY install-emulator.sh /tmp/install-emulator.sh
RUN /bin/bash /tmp/install-emulator.sh && rm /tmp/install-emulator.sh
ENV HOME=/home/emulator
RUN mkdir -p /home/emulator/.aws/emulator
EXPOSE 9297
ENTRYPOINT ["aws-emulator", "--bind", "0.0.0.0", "--port", "9297", "--telemetry", "off"]
