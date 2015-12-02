import os
from xml.etree import ElementTree
from random import randint, choice
import subprocess
import datetime as dt
from multiprocessing import Process, Pool, freeze_support

maps = [
    'england',
    'france',
    'switzerland',
    'the_netherlands'
    ]

#MAP = 'the_netherlands'

PATH = 'config/'
PREFIX = 'centralized_'

def wrap_mult_args(args):
    process(*args)

def process(conf,map_name):
    print conf
    #Open it, create a random seed, save it with new name
    document = ElementTree.parse(PATH + PREFIX +  map_name + ".xml")
    seed = 0
    for elem in document.findall( 'tasks' ):
        seed = str(randint(10000,9999999))
        elem.attrib['rngSeed'] = seed
        break
    
    new_settings_file = PATH + map_name + '_conf=%s_seed=%s.xml' % ('+'.join(map(str,conf)), str(seed))
    
    for elem in document.findall( 'companies/company/vehicle' ):
        if conf:
            for sub_elem in elem.findall(".//*[@capacity]"):            
                cap = conf.pop(0)
                sub_elem.attrib['capacity'] = str(cap)
                break
        else:
            break

    document.write(new_settings_file)
    
    #Then give that as an argument to .jar application.
    begin_time = dt.datetime.now()
    process = subprocess.Popen("java -jar  -Xms1024m -Xmx1024m -Duser.country=US -Duser.language=en centralized.jar %s centralized" % new_settings_file, shell=True)
    #print process.communicate()
    process.wait()
    end_time = dt.datetime.now()
    os.remove(new_settings_file)
    print "SEED:", seed, (end_time - begin_time).total_seconds(),"seconds"


if __name__ == '__main__':
    freeze_support()
    from itertools import chain, combinations,product
    from pprint import pprint as pp
    
    sizes = [5,10,15]
    #vehicle_size_confs = [((choice(sizes),(choice(sizes)) for a in [1,2]]
    vehicle_size_confs = list([ [choice(sizes)]*3 for x in xrange(2)])
    confs = list(product(vehicle_size_confs,maps))
    confs = [(list(x[0]),str(x[1])) for x in confs]
    
    beg = dt.datetime.now()
    pool = Pool(2)
    pool.map(wrap_mult_args, confs)
    pool.close()
    end = dt.datetime.now()
    print "TOTAL",(end - beg).total_seconds(),"seconds"
    
    
