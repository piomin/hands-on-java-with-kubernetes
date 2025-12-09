# Hands-On Java with Kubernetes [![Twitter](https://img.shields.io/twitter/follow/piotr_minkowski.svg?style=social&logo=twitter&label=Follow%20Me)](https://twitter.com/piotr_minkowski)

[![CircleCI](https://circleci.com/gh/piomin/hands-on-java-with-kubernetes.svg?style=svg)](https://circleci.com/gh/piomin/hands-on-java-with-kubernetes)

[![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-black.svg)](https://sonarcloud.io/dashboard?id=piomin_spring-microservices-kubernetes)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=piomin_spring-microservices-kubernetes&metric=bugs)](https://sonarcloud.io/dashboard?id=piomin_spring-microservices-kubernetes)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=piomin_spring-microservices-kubernetes&metric=coverage)](https://sonarcloud.io/dashboard?id=piomin_spring-microservices-kubernetes)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=piomin_spring-microservices-kubernetes&metric=ncloc)](https://sonarcloud.io/dashboard?id=piomin_spring-microservices-kubernetes)

This repository contains sample Java applications for "Hands-On Java with Kubernetes" book.

To get a book do to the following website: [Hands-On Java with Kubernetes](https://leanpub.com/hands-on-java-with-kubernetes).

<img src="book.png" alt="Cover" style="width: 40%;" />

## Repository structure
The repository is divided into catalogs, each of which is assigned to a specific chapter in the book.

Chapter 1 (`chapter-1`): Run First Applications on Kubernetes
Introduction to running Java applications on Kubernetes.
Includes sample applications in both Spring Boot and Quarkus.

Chapter 3 (`chapter-3`): Best Practices for Running Apps on Kubernetes

Chapter 4 (`chapter-4`): Inner Development Loop

Chapter 5 (`chapter-5`): Security

Chapter 6 (`chapter-6`): Kubernetes Native Integration

Chapter 7 (`chapter-7`): Scalability & Performance

Chapter 8 (`chapter-8`): Service Mesh

Chapter 9 (`chapter-9`): CI/CD on Kubernetes with Tekton and Argo CD

## Running

To run the applications you need to have a Kubernetes cluster. You can use Minikube or any other Kubernetes cluster. You can find all the details in the book.

To build the apps without integration tests run:
```shell
mvn clean package
```

To build the apps with integration tests run the command below. But you need to have in mind that it uses Testcontainers, so you must run Docker or Podman locally.
```shell
mvn clean verify
```
