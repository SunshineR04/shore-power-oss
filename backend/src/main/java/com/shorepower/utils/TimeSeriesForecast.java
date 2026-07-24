package com.shorepower.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Holt-Winters 三次指数平滑预测算法
 *
 * 此算法将时间序列分解为三个分量：
 *   - 水平分量(Level)：当前基准值，指数平滑
 *   - 趋势分量(Trend)：整体的增减趋势，指数平滑
 *   - 季节性分量(Seasonality)：周期性波动，这里是周周期(7天)，乘法模型
 *
 * 为什么选择 Holt-Winters 而不是简单平均？
 * 岸电能耗有明显的周周期特征——工作日船舶作业多，能耗高；
 * 周末港口活动减少，能耗低。简单平均无法捕捉这个规律。
 * Holt-Winters 同时建模了趋势和季节性，预测准确度更高。
 *
 * 平滑常数选择依据：
 *   - alpha=0.3（水平）：中等响应速度，不过度敏感于单日波动
 *   - beta=0.1（趋势）：趋势变化缓慢，需要多天数据确认
 *   - gamma=0.1（季节性）：周模式是稳定结构性特征，不宜单日调整
 *   - period=7：周周期（工作日vs周末）
 *
 * 输出：未来 steps 天的逐日预测值，附带90%置信区间
 */
public class TimeSeriesForecast {

    private static final double ALPHA = 0.3;
    private static final double BETA = 0.1;
    private static final double GAMMA = 0.1;
    private static final int PERIOD = 7;

    /** 预测结果点，包含点预测值和90%置信区间上下界 */
    public static class Point {
        public final double forecast;
        public final double lower;
        public final double upper;

        Point(double forecast, double lower, double upper) {
            this.forecast = forecast;
            this.lower = lower;
            this.upper = upper;
        }
    }

    /**
     * 执行 Holt-Winters 预测
     *
     * @param history 历史能耗数据（至少2个数据点）
     * @param steps   预测步数（未来多少天）
     * @return 预测结果列表，每步一个 Point
     *
     * 算法步骤：
     *   1. 初始化季节性因子（取第一个周期的数据/平均值）
     *   2. 初始化水平值（取第一个周期平均值）
     *   3. 递推更新：水平→趋势→季节性（逐点更新）
     *   4. 计算RMSE（用于置信区间）
     *   5. 外推未来 steps 步：forecast = (level + trend × step) × seasonalFactor
     *   6. 置信区间：±1.645 × RMSE × sqrt(1 + 1/n + i²/n²)
     */
    public static List<Point> forecast(List<BigDecimal> history, int steps) {
        int n = history.size();
        if (n < 2) return new ArrayList<>();
        double[] vals = history.stream().mapToDouble(BigDecimal::doubleValue).toArray();

        // 初始化季节性因子：取第一个周期数据除以平均值
        // 例如周一值/7天均值=1.25 表示周一比平均高25%
        double[] season = new double[PERIOD];
        if (n >= PERIOD) {
            double avg = avg(vals, 0, PERIOD);
            for (int i = 0; i < PERIOD; i++) season[i] = avg > 0 ? vals[i] / avg : 1;
        } else {
            for (int i = 0; i < PERIOD; i++) season[i] = 1;
        }

        // 水平值初始化为第一个周期的平均值，趋势初始化为0
        double level = avg(vals, 0, Math.min(n, PERIOD));
        double trend = 0;

        // 记录残差用于计算RMSE
        double[] residuals = new double[n];
        for (int i = 0; i < n; i++) {
            int s = i % PERIOD;          // 当前在周几（0~6）
            double lastLevel = level;
            double seasonal = season[s];

            // 更新水平值：去季节化后指数平滑
            level = ALPHA * (vals[i] / seasonal) + (1 - ALPHA) * (level + trend);
            // 更新趋势值：基于水平变化量指数平滑
            trend = BETA * (level - lastLevel) + (1 - BETA) * trend;
            // 更新季节性因子
            season[s] = GAMMA * (vals[i] / level) + (1 - GAMMA) * seasonal;
            // 记录一步预测误差（用于置信区间）
            residuals[i] = vals[i] - (lastLevel + trend) * season[s];
        }

        // RMSE = 均方根误差，反映预测与实际的偏差程度
        double rmse = Math.sqrt(sumSq(residuals) / n);

        // 外推预测
        List<Point> result = new ArrayList<>();
        for (int i = 1; i <= steps; i++) {
            int s = (n + i - 1) % PERIOD;   // 预测日对应的星期几
            double f = (level + trend * i) * season[s];
            // 预测标准误差（随预测步长增大而增加）
            double se = rmse * Math.sqrt(1 + 1.0 / n + (double) (i * i) / (n * n));
            // 90%置信区间：z=1.645（正态分布单侧5%）
            result.add(new Point(f, f - 1.645 * se, f + 1.645 * se));
        }
        return result;
    }

    private static double avg(double[] arr, int start, int len) {
        double s = 0;
        for (int i = start; i < start + len; i++) s += arr[i];
        return s / len;
    }

    private static double sumSq(double[] arr) {
        double s = 0;
        for (double v : arr) s += v * v;
        return s;
    }
}
