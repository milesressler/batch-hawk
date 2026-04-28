FROM public.ecr.aws/amazoncorretto/amazoncorretto:25
COPY api/build/libs/batch-hawk-*.jar /opt/batchhawk/batch-hawk.jar
EXPOSE 8080
WORKDIR /opt/batchhawk
ENTRYPOINT ["java", "-jar", "batch-hawk.jar"]
