FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive
ENV MYSQL_ROOT_PASSWORD=rootpassword
ENV MYSQL_DATABASE=quizmanegment

# Install OpenJDK 21, Maven, MySQL Server, and Supervisor
RUN apt-get update && apt-get install -y \
    openjdk-21-jdk \
    maven \
    mysql-server \
    supervisor \
    && rm -rf /var/lib/apt/lists/*

# Configure MySQL
RUN service mysql start && \
    mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '${MYSQL_ROOT_PASSWORD}'; FLUSH PRIVILEGES;" && \
    mysql -u root -p${MYSQL_ROOT_PASSWORD} -e "CREATE DATABASE IF NOT EXISTS ${MYSQL_DATABASE};"

# Copy Application Source
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Supervisor Configuration to run MySQL & Java concurrently
# Using a wildcard (*.jar) so it runs regardless of the exact project name in pom.xml
RUN echo '[supervisord]\nnodaemon=true\n' > /etc/supervisor/conf.d/supervisord.conf && \
    echo '[program:mysql]\ncommand=/usr/bin/mysqld_safe\nautostart=true\nautorestart=true\n' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo '[program:java]\ncommand=sh -c "java -jar /app/target/*.jar"\nautostart=true\nautorestart=true\n' >> /etc/supervisor/conf.d/supervisord.conf

EXPOSE 8080 3306

CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]