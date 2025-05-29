# Dockerfile personalizado para Jenkins agent
FROM maven:3.9.6-eclipse-temurin-11

ENV DEBIAN_FRONTEND=noninteractive

# Cambiar a un mirror confiable para evitar problemas de red
RUN sed -i 's|http://archive.ubuntu.com/ubuntu|http://mirror.math.princeton.edu/pub/ubuntu|' /etc/apt/sources.list && \
    apt-get clean && apt-get update --fix-missing

RUN apt-get install -y \
    docker.io \
    docker-compose \
    curl \
    apt-transport-https \
    lsb-release \
    gnupg \
    && curl -sL https://aka.ms/InstallAzureCLIDeb | bash \
    && curl -LO "https://dl.k8s.io/release/$(curl -sL https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" \
    && install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl \
    && rm kubectl \
    # Instalar GitHub CLI (gh)
    && curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | gpg --dearmor -o /usr/share/keyrings/githubcli-archive-keyring.gpg \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | tee /etc/apt/sources.list.d/github-cli.list > /dev/null \
    && apt-get update \
    && apt-get install -y gh
