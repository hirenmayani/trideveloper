# Introduction #

This is a small application and supporting custom workflow task which allows
TRIRIGA to execute Pentaho Data Integration (Kettle) based ETL jobs and
transformations on a scheduled or on-demand basis.


# Details #

This custom task supports execution of Kettle Jobs ("`*`.kjb") and Transformations ("`*`.ktr") within TRIRIGA.  Support is provided for passing variables to jobs/transformations, as well as scheduled execution via the TRIRIGA scheduling engine.