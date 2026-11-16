FROM public.ecr.aws/aws-cli/aws-cli:latest
ENV AWS_PAGER=""
CMD ["cloudformation", "deploy", "--stack-name", "acceptance-demo", "--template-file", "/bootstrap/resources.yaml", "--no-fail-on-empty-changeset"]
