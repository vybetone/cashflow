package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.CashFlowViewModel
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ViewModelInstantiationTest {

    @Test
    fun testViewModelInstantiation() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = CashFlowViewModel(app)
        assertNotNull(viewModel)
    }
}
