select
  scope_type,
  scope_value
from uc4_report_subscription
where active = 1
order by scope_type, scope_value
