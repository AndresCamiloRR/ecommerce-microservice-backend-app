FROM postman/newman:latest

# Instala el reporter adicional
RUN npm install -g newman-reporter-htmlextra
WORKDIR /etc/newman
COPY E2E.postman_collection.json collection.json
RUN mkdir -p reports

ENTRYPOINT ["newman"]
CMD ["run", "collection.json", "-r", "cli,htmlextra", "--reporter-htmlextra-export", "reports/report.html", "--disable-unicode"]
