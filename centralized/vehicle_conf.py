import os
from xml.etree import ElementTree
from random import randint
import subprocess
import datetime as dt
from multiprocessing import Process, Pool, freeze_support

MAP = 'the_netherlands'

PATH = 'config/'
PREFIX = 'centralized_'


def process(conf):
    print conf
    #Open it, create a random seed, save it with new name
    document = ElementTree.parse(PATH + PREFIX +  MAP + ".xml")
    seed = 0
    for elem in document.findall( 'tasks' ):
        seed = str(randint(10000,9999999))
        elem.attrib['rngSeed'] = seed
        break
    
    new_settings_file = PATH + MAP + '_conf=%s.xml' % '+'.join(map(str,conf))
    
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
    def give_fives():
        return map(lambda x: (x+1)*5, range(6))
    set_of_confs = set()
    all_confs = product(give_fives(), give_fives(),give_fives(),give_fives(),give_fives())
    for v_conf in all_confs:
        set_of_confs.add(tuple(sorted(v_conf)))
    #pp(set_of_confs)
    #print len(set_of_confs)
    confs_as_list = map(list, set_of_confs)
    #pp(confs_as_list)

    beg = dt.datetime.now()
    pool = Pool()
    pool.map(process, confs_as_list)
    pool.close()
    end = dt.datetime.now()
    print "TOTAL",(end - beg).total_seconds(),"seconds"
    
    
