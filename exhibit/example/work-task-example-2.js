{
   types: {
       "Work Task": {
           pluralLabel: "Work Tasks"
       }
   },
   properties: {
       priority: {
           label: "Task Priority",
           valueType: "text"
       },
       building: {
           label: "Building",
           valueType: "text"
       },
       latitude: {
           label: "Latitude",
           valueType: "number"
       },
       longitude: {
           label: "Longitude",
           valueType: "number"
       },
       "planned-end-date": {
           label: "Planned End Date",
           valueType: "date"
       },
       "planned-start-date": {
           label: "Planned Start Date",
           valueType: "date"
       }
   },
   items: [
       {
           label: "1001201-Replace air filters",
           type: "Work Task",
           priority: "Low",
           building: "Headquarters",
           latitude: 42.335743,
           longitude: -71.105138,
           "planned-start-date": "2009-10-22T08:00:00-05:00",
           "planned-end-date": "2009-10-24T17:00:00-05:00"
       },
       {
           label: "1001022-Fire extinguisher check",
           type: "Work Task",
           priority: "High",
           building: "Tasker Parks",
           latitude: 43.422,
           longitude: -72.211,
           "planned-start-date": "2009-10-20T09:00:00-05:00",
           "planned-end-date": "2009-10-21T13:30:00-05:00"
       },
       {
           label: "1001233-Move furniture",
           type: "Work Task",
           priority: "Medium",
           building: "Tasker Parks",
           latitude: 43.422,
           longitude: -72.211,
           "planned-start-date": "2009-10-19T08:00:00-05:00",
           "planned-end-date": "2009-10-22T14:00:00-05:00"
       },
       {
           label: "1001227-Change lightbulbs",
           type: "Work Task",
           priority: "Low",
           building: "Westland II",
           latitude: 41.509,
           longitude: -73.011,
           "planned-start-date": "2009-10-23T13:00:00-05:00",
           "planned-end-date": "2009-10-23T16:30:00-05:00"
       },
       {
           label: "1001332-Fix clogged pipe",
           type: "Work Task",
           priority: "High",
           building: "Westland I",
           latitude: 41.509,
           longitude: -72.009,
           "planned-start-date": "2009-10-19T10:00:00-05:00",
           "planned-end-date": "2009-10-19T12:00:00-05:00"
       }
   ]
}
