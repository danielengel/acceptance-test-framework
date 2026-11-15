FROM public.ecr.aws/amazonlinux/amazonlinux:2023
RUN dnf install -y nginx java-25-amazon-corretto-headless openssl && dnf clean all
COPY start.sh nginx.conf /bootstrap/
EXPOSE 9443
ENTRYPOINT ["/bin/bash", "/bootstrap/start.sh"]
