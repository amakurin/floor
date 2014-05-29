cd /home/floor/floor16
git pull origin
rm -R target/*
rm -R resources/public/js/out/*
sudo service floor stop
rm resources/public/js/site2.js
rm resources/public/js/site-prod.js
lein cljsbuild once dev2
lein cljsbuild once prod
java -jar depl/closure-stylesheets.jar --allowed-non-standard-function progid:DXImageTransform.Microsoft.Alpha --output-file resources/public/css/prod.css resources/public/css/base.css resources/public/css/layout.css resources/public/css/skeleton.css
lein uberjar
sudo service floor start
sudo service nginx restart
