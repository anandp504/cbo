alter table DailyImpressionBudgetHistory
   add column lifetime_budget int null default null after budget;

alter table DailyImpressionBudgetHistory
   change column budget daily_budget int;
