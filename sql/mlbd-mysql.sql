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
    pmid bigint primary key,
    title varchar(4000),
    abstract text,
    doi varchar(256)
    );

create table protein_target (
    accession varchar(20) primary key,    
    gene_id bigint,
    name varchar(4000),
    taxid bigint,
    description varchar(4000),
    uniprot_status varchar(16)
    );

create table compound (
    cid bigint primary key,
    molfile longtext,
    creation date,
    probe_id varchar(128),
    url varchar(4000)
    );

create table substance (
    sid bigint primary key,
    cid bigint,
    creation date
    );

-- store synonyms for compounds, genes
create table synonyms (
    id bigint not null,
    type bigint not null, -- 1 is CID, 2 is geneid
    syn varchar(4000) not null
    );

-- project management tables
create table source (
    source_id bigint primary key,
    name varchar(4000)
    );
create table source_substance (
    source_id bigint not null,
    sid bigint not null,
--    constraint source_id_fk foreign key (source_id) references source (source_id)
    );

-- assay related tables
-- 
-- the classification field in the MLBD schema seems a lot like a BAO annotation
-- kept it here but might be a good idea ot move it out of assay and anticipate it goes into annotation. Similarly
-- type could be moved to an annotation table. But this then requires that we always perform joins
-- to get at that information. So for now we keep it here
create table assay (
    aid bigint primary key,
    name varchar(4000),
    description longtext,
    source varchar(1024),
    category bigint, -- mlscn (1), mlpcn (2), mlscn-ap (3), mlpcn-ap (4)
    type bigint, -- other (0), screening (1), confirmatory (2),  summary (3)
    summary bigint, -- parent summary AID (null if this assay is a summary assay)
    assays bigint,
    grant_no varchar(1024),
    data blob default null,
    deposited date,
    classification bigint,
    samples bigint,
    updated datetime
    );

create table assay_target (
    aid bigint not null,
    accession varchar(20),
    gene_id bigint,
--    constraint aid_fk foreign key (aid) references assay (aid),
--    constraint acc_fk foreign key (accession) references protein_target (accession)
    );

create table assay_pub (
    aid bigint not null,
    pmid bigint not null,
--    constraint ap_aid_fk foreign key (aid) references assay (aid),
--    constraint pmid_fk foreign key (pmid) references publication (pmid)
    );    

-- assay_data_id + updated lets us implement versioning
-- A given aid+sid can have multiple entries, representing versions of a data point
-- Replicate information for a given assay_data_id is contained in assay_result
create table assay_data (
    assay_data_id bigint primary key,
    aid bigint not null,
    sid bigint not null,
    cid bigint,
    classification tinyint, -- substance acquisition classification: mlsmr (null or 0), purchased (1), synthesized (2)
    updated date,
    runset varchar(128) default 'default',
--    constraint ad_aid_fk foreign key (aid) references assay (aid),
--    constraint ad_sid_fk foreign key (sid) references substance (sid)
    );

create table assay_result (
    assay_result_id bigint primary key,
    assay_data_id bigint,
    replicate_id int default 1,
    potency float,
    outcome tinyint,
    score tinyint,
    s0 float, 
    sInf float,
    hill float,
    lac50 float,

--    constraint ar_adi_fk foreign key (assay_data_id) references assay_data (assay_data_id)
    );

create table assay_concentration (
    assay_result_id bigint,
    concentration float,
    response float,
    conc_order bigint default 1,

--    constraint ac_arid_fk foreign key (assay_result_id) references assay_result (assay_result_id)
    );


-- some explicit indexes
--create index assay_summary on assay(summary) tablespace bard_index;