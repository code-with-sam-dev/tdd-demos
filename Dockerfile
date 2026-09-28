# Runnable end to end with one command, on a machine with nothing installed.
#
# The dependency layer is cached separately from the source, so re-running
# after an edit does not re-download JUnit. That matters here more than usual:
# the whole episode is about a cycle you run over and over.
FROM maven:3.9-eclipse-temurin-21

WORKDIR /tdd
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
COPY capture.sh ./capture.sh
RUN chmod +x capture.sh

CMD ["mvn", "-B", "test"]
