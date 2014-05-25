lein cljsbuild once dev2
lein cljsbuild once prod
lein uberjar
sudo service floor restart
sudo service nginx restart
