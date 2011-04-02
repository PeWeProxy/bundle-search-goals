class SampleMigration < ActiveRecord::Base
  def self.up
    create_table :sample do |t|
      t.string :name
    end
  end

  def self.down
    remove_table :sample
  end
end
