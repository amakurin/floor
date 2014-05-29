cd /home/floor/caterpillar
sudo service caterpillar stop
git pull origin
lein clj-sql-up migrate
sudo service caterpillar start
