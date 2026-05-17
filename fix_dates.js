db.reviews.find({ 'created_at.$date': { '$exists': true } }).forEach(function(doc) {
  db.reviews.updateOne(
    { _id: doc._id },
    {
      '$set': {
        created_at: new ISODate(doc.created_at['$date']),
        updated_at: new ISODate(doc.updated_at['$date'])
      }
    }
  );
})
