import os
from xml.etree import ElementTree
from random import randint
import subprocess
import datetime as dt
from multiprocessing import Process, Pool, freeze_support


maps = [
    'england',
    'france',
    'switzerland',
    'the_netherlands'
    ]

PATH = 'config/'
PREFIX = 'centralized_'


def process(map_name):
    print map_name, "!!"
    #Open it, create a random seed, save it with new name
    document = ElementTree.parse(PATH + PREFIX +  map_name + ".xml")
    seed = 0
    for elem in document.findall( 'tasks' ):
        seed = str(randint(10000,9999999))
        elem.attrib['rngSeed'] = seed
        break
    new_settings_file = PATH + map_name + '_%s.xml' % seed
    document.write(new_settings_file)
    #Then give that as an argument to .jar application.
    begin_time = dt.datetime.now()
    process = subprocess.Popen("java -jar  -Xms1024m -Xmx1024m -Duser.country=US -Duser.language=en centralized.jar %s centralized" % new_settings_file, shell=True)
    #print process.communicate()
    process.wait()
    end_time = dt.datetime.now()
    os.remove(new_settings_file)
    #print "SEED:", seed, (end_time - begin_time).total_seconds(),"seconds"


if __name__ == '__main__':
    freeze_support()
    beg = dt.datetime.now()
    pool = Pool()
    pool.map(process, maps*500)
    pool.close()
    end = dt.datetime.now()
    print "TOTAL",(end_time - begin_time).total_seconds(),"seconds"
    
    
