FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive
ENV MYSQL_ROOT_PASSWORD=rootpassword
ENV MYSQL_DATABASE=quiz_db

# Install OpenJDK 17, Maven, MySQL Server, and Supervisor
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
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
RUN echo '[supervisord]\nnodaemon=true\n' > /etc/supervisor/conf.d/supervisord.conf && \
    echo '[program:mysql]\ncommand=/usr/bin/mysqld_safe\nautostart=true\nautorestart=true\n' >> /etc/supervisor/conf.d/supervisord.conf && \
    echo '[program:java]\ncommand=java -jar /app/target/quiz-management-backend-0.0.1-SNAPSHOT.jar\nautostart=true\nautorestart=true\n' >> /etc/supervisor/conf.d/supervisord.conf

EXPOSE 8080 3306

CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]