FROM ubuntu:latest
LABEL authors="davisilva-ieg"

ENTRYPOINT ["top", "-b"]