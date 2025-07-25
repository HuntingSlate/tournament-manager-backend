FROM ubuntu:20.04

ENV TZ=Europe/Warsaw
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN apt-get update && apt-get install -y cron curl gnupg

RUN curl https://packages.microsoft.com/keys/microsoft.asc | apt-key add -
RUN curl https://packages.microsoft.com/config/ubuntu/20.04/prod.list > /etc/apt/sources.list.d/mssql-release.list
RUN apt-get update && ACCEPT_EULA=Y apt-get install -y mssql-tools unixodbc-dev

WORKDIR /app
COPY ./sql /app/sql
COPY ./scripts /app/scripts
COPY ./crontab.txt /etc/cron.d/sql-maintenance-cron
COPY ./entrypoint.sh /app/entrypoint.sh

RUN sed -i 's/\r$//g' /app/entrypoint.sh && sed -i 's/\r$//g' /app/scripts/*.sh
RUN chmod +x /app/entrypoint.sh && chmod +x /app/scripts/*.sh
RUN chmod 0644 /etc/cron.d/sql-maintenance-cron

CMD ["/bin/bash", "/app/entrypoint.sh"]