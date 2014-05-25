lein cljsbuild once dev2
lein cljsbuild once prod
java -jar depl/closure-stylesheets.jar --allowed-non-standard-function progid:DXImageTransform.Microsoft.Alpha --output-file resources/public/css/prod.css resources/public/css/base.css resources/public/css/layout.css resources/public/css/skeleton.css
lein uberjar
sudo service floor restart
sudo service nginx restart
