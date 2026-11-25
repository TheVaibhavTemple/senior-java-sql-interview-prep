# SQL — Window Functions

5 problem sets. Part of [senior-java-sql-interview-prep](../../README.md).

| ID | Question | Level | Weightage | Priority | Technique | File |
|---|---|---|---|---|---|---|
| S09 | Top N highest-paid employees in each department | Medium | High | P1 | ROW_NUMBER / RANK / DENSE_RANK over PARTITION BY, filtered in an outer query | [`S09_top_n_per_group.sql`](S09_top_n_per_group.sql) |
| S10 | Running total, moving average, and percent of total | Medium | High | P1 | Aggregate OVER a window frame (ROWS BETWEEN ... AND CURRENT ROW) | [`S10_running_total_and_moving_average.sql`](S10_running_total_and_moving_average.sql) |
| S11 | LAG and LEAD -- month-over-month change and first salary increase | Medium | High | P2 | LAG / LEAD to reach the previous or next row without a self-join | [`S11_lag_lead_period_over_period.sql`](S11_lag_lead_period_over_period.sql) |
| S23 | Employees earning more than their department average | Easy-Medium | High | P1 | Compare each row with its group aggregate -- correlated subquery, AVG() OVER (PARTITION BY), or JOIN to a GROUP BY derived table | [`S23_salary_above_department_average.sql`](S23_salary_above_department_average.sql) |
| S25 | Running balance per account (the balance after every transaction) | Medium | High | P1 | Signed amount with CASE, then SUM() OVER (PARTITION BY account_id ORDER BY txn_ts, txn_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) | [`S25_running_balance_per_account.sql`](S25_running_balance_per_account.sql) |

## Is folder ko kaise padhein

Pehle S23 aur S09 karo -- dono P1 hain aur lagbhag har round mein aate hain. Phir S10 ka running
total, aur RANGE vs ROWS ka farak bolna seekho. S11 (LAG/LEAD) P2 hai; product ya fintech round
ho to zaroor karo. Har file mein pehle SAMJHO padho, phir query bina dekhe type karo.

## Mock round

| Interviewer | Aap |
|---|---|
| Find the employees who earn more than their department's average. | First I'd confirm strictly greater, and whether employees without a department count. Then a correlated subquery: `salary > (SELECT AVG(salary) FROM employees e2 WHERE e2.dept_id = e.dept_id)`. |
| Can you do it without the correlated subquery? | Yes, `AVG(salary) OVER (PARTITION BY dept_id)` in a derived table, then I filter `salary > dept_avg` outside. It is one scan plus a sort, and I can show the average on every row. |
| Why not put the window function straight into WHERE? | Window functions are computed after WHERE, in the SELECT step, so WHERE cannot see them. I wrap the query in a CTE or a derived table and filter there. |
| Now the top 2 earners per department. Two people tie for second. | It depends on the tie rule. ROW_NUMBER with a unique tiebreaker like emp_id gives exactly two rows; DENSE_RANK keeps both tied people, so that department returns three. |
| Give me a running total of revenue by date. | `SUM(amount) OVER (ORDER BY order_date, order_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)`. I add order_id so the order is unique. |
| Why did you write ROWS? It works without it. | Without a frame the default is RANGE, so rows that tie on the ORDER BY value share one total. ROWS makes it strictly row by row. |
| Now a running balance per account. A transfer and its fee post in the same second. | A CASE turns DEBIT into a negative amount, then `SUM(signed_amount) OVER (PARTITION BY account_id ORDER BY txn_ts, txn_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)`. Without txn_id both rows would show the balance after the fee. |
| Month-over-month revenue change? | I aggregate to one row per month, then `revenue - LAG(revenue) OVER (ORDER BY month)`, with NULLIF on the denominator for the percentage. If a month can be missing I join a calendar first, because LAG gives the previous row, not the previous month. |
| How does this behave on 10 million rows? | Each window is one scan plus a sort on the PARTITION BY and ORDER BY columns, and an index in that order can remove the sort. I would check the plan rather than guess. |

## Rapid fire

| Sawaal | Jawab |
|---|---|
| Window function aur GROUP BY mein farak? | GROUP BY rows collapse karta hai; window har row rakhta hai aur aggregate saath mein deta hai. |
| ROW_NUMBER vs RANK vs DENSE_RANK (100, 90, 90, 80)? | 1,2,3,4 vs 1,2,2,4 vs 1,2,2,3 -- tie ke baad RANK gap chhodta hai, DENSE_RANK nahi. |
| Window function WHERE mein kyun nahi? | WHERE pehle chalta hai, window SELECT ke time banta hai; CTE mein wrap karo. |
| ORDER BY hai par frame nahi, to default? | RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW. |
| ORDER BY bhi nahi, to? | Poori partition ek frame hai -- percent of total isi se banta hai. |
| 3-row moving average ka frame? | ROWS BETWEEN 2 PRECEDING AND CURRENT ROW. |
| Running total har customer pe reset? | PARTITION BY cust_id ORDER BY order_date, order_id. |
| Account pehli baar negative kab hua? | Running balance CTE mein banao, bahar balance < 0 filter karo, phir ROW_NUMBER() = 1. |
| Pehli row pe LAG kya deta hai? | NULL; LAG(x, 1, 0) se default do. |
| LAG ko previous month samajhna galat kyun? | LAG previous ROW deta hai; missing month ke liye pehle date spine chahiye. |
| AVG(salary) vs SUM(salary) / COUNT(*)? | AVG NULL skip karta hai; SUM / COUNT(*) NULL bhi ginta hai, aur SQLite / Postgres mein integer division hai. |
| Har row pe department average? | AVG(salary) OVER (PARTITION BY dept_id). |
| Har row pe company average? | AVG(salary) OVER () -- khaali OVER. |

## Run them

```bash
cd sql && python3 run_all.py
```

That rebuilds `prep.db` from `00-schema/`, runs every `-- >>> QUERY` block and
rewrites the results back into each file. A fresh database is built **per file**,
so every file reproduces standalone.
