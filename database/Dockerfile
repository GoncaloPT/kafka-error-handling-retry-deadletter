FROM postgres:12.1-alpine
COPY init.sql /docker-entrypoint-initdb.d
ENV POSTGRES_PASSWORD=docker
ENV POSTGRES_USER=docker
ENV POSTGRES_DB=db
