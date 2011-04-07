
class CreateSearchGoalsSchema < ActiveRecord::Migration
  def self.up
    create_table :searchgoals_searches do |t|
      t.integer :id, :limit => 11
      t.string :uid, :limit => 32
      t.datetime :timestamp
      t.string :search_query, :limit => 300
      t.string :goal, :limit => 100
    end

    add_index :searchgoals_searches, :uid, [:id, :uid]

    create_table :searchgoals_search_results do |t|
      t.integer :id, :limit => 11
      t.string :url, :limit => 4000
      t.string :heading, :limit => 200
      t.string :perex, :limit => 500
      t.integer :id_search, :limit => 11
    end

    add_index :searchgoals_search_results, :id_search

    create_table :searchgoals_clicked_results do |t|
      t.integer :id, :limit => 11
      t.datetime :timestamp
      t.integer :id_search_result	, :limit => 11
    end

    add_index :searchgoals_clicked_results, :id_search_result
  end

  def self.down
    drop_table :searchgoals_searches
    drop_table :searchgoals_search_results
    drop_table :searchgoals_clicked_results
  end
end