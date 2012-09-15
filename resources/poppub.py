import urllib, sys
import xml.etree.ElementTree as ET
import MySQLdb as m

def quote(s):
    if s is None: return 'null'
    else: 
        return "'"+m.escape_string(s.encode('ascii', 'ignore'))+"'"

con = m.connect("protein.nhgri.nih.gov","guhar","beeb1e","bard2")
cur = con.cursor()

cur.execute("select distinct pmid from assay_pub")
pmids = [x[0] for x in cur.fetchall()]

cur.execute("select distinct pmid from target_pub")
pmids.extend([x[0] for x in cur.fetchall()])

print 'Got %d PMIDs' % (len(set(pmids)))

## now get set of pmids in publications
cur.execute("select distinct pmid from publication")
pubids = [x[0] for x in cur.fetchall()]
print 'Got %d PMIDs with publication data' % (len(pubids))

newids = list(set(pmids) - set(pubids))
newids = filter(lambda x: x != 0, newids)
print 'Will retrieve data for %d PMIDs' % (len(newids))

base_url = 'http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.cgi?db=pubmed&retmode=xml&id='
newdata = []
n = 0
for pmid in newids:
    if pmid == 0: continue
    url = base_url+str(pmid)
    page = urllib.urlopen(url).read()
    doc = ET.fromstring(page)
    
    title = doc.find('PubmedArticle/MedlineCitation/Article/ArticleTitle')
    if title is not None: title = title.text

    abs = doc.find('PubmedArticle/MedlineCitation/Article/Abstract/AbstractText')
    if abs is not None: abs = abs.text

    doi = doc.find("PubmedArticle/PubmedData/ArticleIdList/ArticleId[@IdType='doi']")
    if doi is not None: doi = doi.text

    sql = "insert into publication (pmid, title, abstract, doi) values (%d, %s, %s, %s)" % (pmid, quote(title), quote(abs), quote(doi))
                
    cur.execute(sql)

    n += 1
    if n % 10 == 0:
        sys.stdout.write("\rProcessed %d" % (n))
        sys.stdout.flush()
        con.commit()
print        

