#!/usr/bin/env python2.7

import argparse
import calendar
from datetime import datetime

import fdb

parser = argparse.ArgumentParser(prog='clean_tx_fdb.py')
parser.add_argument('--time', help='now or in yyyyMM format', default='now', required=False)

args = parser.parse_args()

date = datetime.utcnow();
if 'now' != args.time:
    date = datetime.strptime(args.time, '%Y%m').strftime('%Y%m')
else:
    date = date.strftime('%Y%m')
print (date);

fdb.api_version(520)
db = fdb.open()

id_dir = ('Transaction', 'id', date);
success = fdb.directory.remove_if_exists(db, id_dir)
print 'remove id directory {} {}'.format(id_dir, success)
ext_dir = ('Transaction', 'ext', date);
success = fdb.directory.remove_if_exists(db, ext_dir)
print 'remove ext directory {} {}'.format(id_dir, success)

cal = datetime.strptime(date, '%Y%m')

num_days = calendar.monthrange(cal.year, cal.month)[1]

for day in range(1, num_days + 1):
    dt = datetime(cal.year, cal.month, day)
    day_dir = ('Transaction', dt.strftime('%Y%m%d'));
    success = fdb.directory.remove_if_exists(db, day_dir)
    print 'remove by day directory {} {}'.format(day_dir, success)
