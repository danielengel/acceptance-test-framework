FROM public.ecr.aws/amazonlinux/amazonlinux:2023
RUN dnf install -y awscli-2 && dnf clean all
ENV AWS_PAGER=""
ENTRYPOINT ["aws"]
CMD ["cloudformation", "deploy", "--stack-name", "acceptance-demo", "--template-file", "/bootstrap/resources.yaml", "--no-fail-on-empty-changeset"]
