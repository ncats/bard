drop table assay_concentration;
drop table assay_result;
drop table assay_data;
drop table assay_target;
drop table assay_pub;
drop table assay;
drop table synonyms;
drop table substance;
drop table source_substance;
drop table source;
drop table compound;
drop table protein_target;
drop table publication;

create table publication (
    pmid number primary key,
    title varchar2(4000),
    abstract clob,
    doi varchar2(256)
    );

create table protein_target (
    accession varchar2(20) primary key,    
    gene_id number,
    name varchar2(4000),
    taxid number,
    description varchar2(4000),
    uniprot_status varchar2(16)
    );

create table compound (
    cid number primary key,
    molfile clob,
    creation date,
    probe_id varchar2(128),
    url varchar2(4000)
    );

create table substance (
    sid number primary key,
    cid number,
    creation date
    --constraint cid_fk foreign key (cid) references compound (cid)
    );

-- store synonyms for compounds, genes
create table synonyms (
    id number not null,
    type number not null, -- 1 is CID, 2 is geneid
    syn varchar2(4000) not null
    );

-- project management tables
create table source (
    source_id number primary key,
    name varchar2(4000)
    );
create table source_substance (
    source_id number not null,
    sid number not null,
    constraint source_id_fk foreign key (source_id) references source (source_id)   
    );

-- assay related tables
-- 
-- the classification field in the MLBD schema seems a lot like a BAO annotation
-- kept it here but might be a good idea ot move it out of assay and anticipate it goes into annotation. Similarly
-- type could be moved to an annotation table. But this then requires that we always perform joins
-- to get at that information. So for now we keep it here
create table assay (
    aid number primary key,
    name varchar2(4000),
    description clob,
    source varchar2(1024),
    assays number,
    category number, -- mlscn (1), mlpcn (2), mlscn-ap (3), mlpcn-ap (4)
    type number, -- other (0), screening (1), confirmatory (2),  summary (3)
    summary number, -- parent summary AID (null if this assay is a summary assay)
    grant_no varchar2(1024),
    deposited date,
    updated date,
    data blob default null,
    classification number,
    samples number
    );

create table assay_target (
    aid number not null,
    accession varchar2(20),
    gene_id number,
    constraint aid_fk foreign key (aid) references assay (aid),
    constraint acc_fk foreign key (accession) references protein_target (accession)
    );

create table assay_pub (
    aid number not null,
    pmid number not null,
    constraint ap_aid_fk foreign key (aid) references assay (aid),
    constraint pmid_fk foreign key (pmid) references publication (pmid)
    );    

-- assay_data_id + updated lets us implement versioning
-- A given aid+sid can have multiple entries, representing versions of a data point
-- Replicate information for a given assay_data_id is contained in assay_result
create table assay_data (
    assay_data_id number primary key,
    aid number not null,
    sid number not null,
    cid number not null,
    classification number, -- substance acquisition classification: mlsmr (null or 0), purchased (1), synthesized (2)
    updated date,
    runset varchar2(128) default 'default',
    constraint ad_aid_fk foreign key (aid) references assay (aid),
    constraint ad_cid_fk foreign key (cid) references compound (cid),
    constraint ad_sid_fk foreign key (sid) references substance (sid)
    );

create table assay_result (
    assay_result_id number primary key,
    assay_data_id number,
    replicate_id number default 1,
    potency float,
    outcome number,
    score number,
    s0 float, 
    sInf float,
    hill float,
    lac50 float,

    constraint ar_adi_fk foreign key (assay_data_id) references assay_data (assay_data_id)
    );

create table assay_concentration (
    assay_result_id number,
    concentration float,
    response float,
    conc_order number default 1,

    constraint ac_arid_fk foreign key (assay_result_id) references assay_result (assay_result_id)
    );
